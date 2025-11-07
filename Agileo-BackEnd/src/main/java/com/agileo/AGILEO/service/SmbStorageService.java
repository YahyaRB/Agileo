package com.agileo.AGILEO.service;


import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.EnumSet;

@Service
public class SmbStorageService {
    private static final Logger log = LoggerFactory.getLogger(SmbStorageService.class);

    @Value("${storage.smb.host:}")
    private String host;

    @Value("${storage.smb.share:}")
    private String share;

    // ex: rb217/fichiers/ficjoints_op
    @Value("${storage.smb.base-path:}")
    private String basePath;

    @Value("${storage.smb.username:}")
    private String username;

    @Value("${storage.smb.password:}")
    private String password;

    // vide si pas d’AD
    @Value("${storage.smb.domain:}")
    private String domain;
    public String getHost()     { return host; }
    public String getShare()    { return share; }
    public String getBasePath() { return basePath; }
    public String getUsernameMasked() {
        return (username == null || username.isBlank()) ? "" : username;
    }
    public boolean testConnection() {
        try {
            AuthenticationContext ac = new AuthenticationContext(
                    username != null ? username : "",
                    (password != null ? password : "").toCharArray(),
                    (domain != null && !domain.isBlank()) ? domain : null
            );

            SMBClient client = new SMBClient();
            try (com.hierynomus.smbj.connection.Connection conn = client.connect(notNull(host))) {
                com.hierynomus.smbj.session.Session session = conn.authenticate(ac);
                try (com.hierynomus.smbj.share.DiskShare disk =
                             (com.hierynomus.smbj.share.DiskShare) session.connectShare(notNull(share))) {

                    // Vérifie que le share répond (ex : existence de basePath si fourni)
                    String checkPath = (basePath == null || basePath.isBlank()) ? null : basePath;
                    if (checkPath != null) {
                        String dir = trim(checkPath);
                        // on essaie une opération inoffensive
                        if (!disk.folderExists(dir)) {
                            // pas bloquant : on considère la connexion OK même si le dossier n'existe pas
                            // (si tu veux être strict, retourne false ici)
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("SMB testConnection() failed: {}", e.getMessage());
            return false;
        }
    }
    @PostConstruct
    public void validateProps() {
        log.info("=== SMB CONFIG === host={}, share={}, basePath={}", host, share, basePath);
        if (isBlank(host))  throw new IllegalStateException("storage.smb.host manquant");
        if (isBlank(share)) throw new IllegalStateException("storage.smb.share manquant");
        // basePath peut être vide, mais on normalise quand même
        basePath = trim(basePath);
    }

    /** Upload depuis MultipartFile vers basePath/<subdir>/<filename> ; renvoie le chemin relatif retourné en BDD */
    public String upload(MultipartFile file, String subdir) throws IOException {
        if (file == null || file.isEmpty()) throw new IOException("Fichier vide");
        String fileName = file.getOriginalFilename();
        if (isBlank(fileName)) throw new IOException("Nom de fichier invalide");
        String dir = joinPaths(basePath, subdir);
        String rel = joinPaths(dir, fileName);
        try (InputStream in = file.getInputStream()) {
            upload(in, (int) file.getSize(), rel);   // réutilise la méthode flux
        }
        return rel; // ex: rb217/fichiers/ficjoints_op/demandes/26648/xxx.pdf
    }

    /** Upload générique d’un flux vers un chemin relatif dans le share (utilisé par ton testSmbConnection) */
    public void upload(InputStream data, int length, String relativePath) throws IOException {
        final String rel = trim(relativePath);       // ← variable finale
        final String parent = parentDir(rel);        // ← variable finale
        withShare(disk -> {
            mkdirs(disk, parent);
            try (File f = disk.openFile(
                    rel,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
            );
                 OutputStream out = f.getOutputStream()) {
                data.transferTo(out);
            }
        });
    }


    /** Téléchargement – retourne un InputStream (à fermer par l’appelant) */
    public InputStream download(String relativePath) throws IOException {
        final String rel = trim(relativePath);       // ← finale
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis  = new PipedInputStream(pos);

        new Thread(() -> {
            try {
                withShare(disk -> {
                    try (File f = disk.openFile(
                            rel,
                            EnumSet.of(AccessMask.GENERIC_READ),
                            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OPEN,
                            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
                    );
                         InputStream in = f.getInputStream()) {
                        in.transferTo(pos);
                    }
                });
            } catch (IOException e) {
                log.error("SMB download error: {}", e.getMessage(), e);
            } finally {
                try { pos.close(); } catch (IOException ignore) {}
            }
        }, "smb-download-" + System.nanoTime()).start();

        return pis;
    }


    /** Suppression – renvoie true si supprimé */
    public boolean delete(String relativePath) throws IOException {
        final String rel = trim(relativePath);       // ← finale
        final boolean[] deleted = {false};
        withShare(disk -> {
            if (disk.fileExists(rel)) {
                disk.rm(rel);
                deleted[0] = true;
            }
        });
        return deleted[0];
    }


    /* ====================== Helpers bas niveau ====================== */

    private interface ShareCallback { void run(DiskShare disk) throws IOException; }

    private void withShare(ShareCallback cb) throws IOException {
        AuthenticationContext ac = new AuthenticationContext(
                notNull(username),
                notNull(password).toCharArray(),
                isBlank(domain) ? null : domain
        );
        SMBClient client = new SMBClient();
        try (Connection conn = client.connect(notNull(host))) {
            Session session = conn.authenticate(ac);
            try (DiskShare disk = (DiskShare) session.connectShare(notNull(share))) {
                cb.run(disk);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void mkdirs(DiskShare disk, String path) {
        if (isBlank(path)) return;
        String[] parts = path.split("[/\\\\]+");
        String cur = "";
        for (String p : parts) {
            if (isBlank(p)) continue;
            cur = cur.isEmpty() ? p : cur + "/" + p;
            if (!disk.folderExists(cur)) disk.mkdir(cur);
        }
    }

    private static String joinPaths(String a, String b) {
        String left = trim(a), right = trim(b);
        if (isBlank(left))  return right;
        if (isBlank(right)) return left;
        return left + "/" + right;
    }
    private static String parentDir(String path) {
        int i = trim(path).lastIndexOf('/');
        return i <= 0 ? "" : trim(path).substring(0, i);
    }
    private static String trim(String p) {
        if (p == null) return "";
        return p.replaceAll("^[/\\\\]+", "").replaceAll("[/\\\\]+$", "");
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String notNull(String s) {
        if (isBlank(s)) throw new IllegalArgumentException("Configuration SMB manquante");
        return s;
    }
}
