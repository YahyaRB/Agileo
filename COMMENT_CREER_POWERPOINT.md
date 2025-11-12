# Comment créer et télécharger le PowerPoint DIVALEO

## ⚠️ Important
Le fichier `PRESENTATION_DIVALEO.md` est un fichier **Markdown** (texte), pas un PowerPoint. Vous devez le convertir en PowerPoint (.pptx).

---

## 🎯 Option 1 : Création manuelle dans PowerPoint (Recommandé)

### Étapes détaillées :

1. **Ouvrir PowerPoint**
   - Ouvrir Microsoft PowerPoint
   - Créer une nouvelle présentation

2. **Pour chaque slide du fichier MD :**
   - Ouvrir `PRESENTATION_DIVALEO.md`
   - Pour chaque section "SLIDE X" :
     - Cliquer sur "Nouvelle diapositive" dans PowerPoint
     - Copier le titre (après "SLIDE X :")
     - Copier le contenu
     - Formater selon vos besoins

3. **Enregistrer le PowerPoint :**
   - Fichier → Enregistrer sous
   - Choisir le format `.pptx`
   - Nommer : `PRESENTATION_DIVALEO.pptx`
   - Cliquer sur "Enregistrer"

**Avantages :** Contrôle total, design personnalisé, ajout facile d'images
**Temps estimé :** 1-2 heures

---

## 🔄 Option 2 : Conversion automatique avec Pandoc

### Installation de Pandoc :

1. **Télécharger Pandoc :**
   - Aller sur : https://pandoc.org/installing.html
   - Télécharger et installer Pandoc

2. **Conversion :**
   ```bash
   pandoc PRESENTATION_DIVALEO.md -o PRESENTATION_DIVALEO.pptx
   ```

3. **Le fichier PowerPoint sera créé automatiquement**

**Avantages :** Rapide, automatique
**Inconvénients :** Moins de contrôle sur le design

---

## 🌐 Option 3 : Outils en ligne (Gratuits)

### A. Marp (Recommandé pour présentations)
1. Aller sur : https://marp.app/
2. Coller le contenu du fichier MD
3. Exporter en PowerPoint

### B. Dillinger.io
1. Aller sur : https://dillinger.io/
2. Importer le fichier MD
3. Exporter en HTML puis convertir en PowerPoint

### C. CloudConvert
1. Aller sur : https://cloudconvert.com/md-to-pptx
2. Uploader `PRESENTATION_DIVALEO.md`
3. Convertir en PPTX
4. Télécharger le fichier

---

## 📝 Option 4 : Utiliser PowerPoint Online

1. **Aller sur :** https://www.office.com/launch/powerpoint
2. **Créer une nouvelle présentation**
3. **Copier-coller le contenu** slide par slide depuis le fichier MD
4. **Enregistrer :**
   - Fichier → Télécharger
   - Choisir "Télécharger une copie"
   - Le fichier .pptx sera téléchargé

---

## 🚀 Option 5 : Script Python (Pour développeurs)

Si vous avez Python installé, vous pouvez utiliser ce script :

```python
# Nécessite : pip install python-pptx markdown

from pptx import Presentation
import re

# Créer une nouvelle présentation
prs = Presentation()

# Lire le fichier MD
with open('PRESENTATION_DIVALEO.md', 'r', encoding='utf-8') as f:
    content = f.read()

# Parser les slides
slides = re.split(r'^## SLIDE \d+', content, flags=re.MULTILINE)

for slide_content in slides[1:]:  # Ignorer le premier élément
    # Créer un nouveau slide
    slide = prs.slides.add_slide(prs.slide_layouts[0])
    
    # Extraire le titre
    title_match = re.search(r':\s*(.+?)\n', slide_content)
    if title_match:
        title = slide_content.split('\n')[0].replace('**', '').strip()
        slide.shapes.title.text = title
    
    # Ajouter le contenu
    # (Simplifié - vous pouvez améliorer le parsing)

# Sauvegarder
prs.save('PRESENTATION_DIVALEO.pptx')
print("PowerPoint créé avec succès !")
```

---

## ✅ Méthode la plus simple (Recommandée)

### Étapes rapides :

1. **Ouvrir PowerPoint**
2. **Ouvrir le fichier `PRESENTATION_DIVALEO.md`** dans un éditeur de texte
3. **Créer les slides un par un :**
   - Slide 1 : Copier le titre et le contenu
   - Slide 2 : Nouvelle diapositive, copier le contenu
   - Et ainsi de suite...
4. **Ajouter les visuels** (captures d'écran, graphiques)
5. **Enregistrer** : Fichier → Enregistrer sous → `PRESENTATION_DIVALEO.pptx`

**Temps estimé :** 1-2 heures pour 25 slides

---

## 📋 Checklist avant de télécharger

- [ ] Tous les 25 slides sont créés
- [ ] Le design est cohérent
- [ ] Les captures d'écran sont ajoutées
- [ ] Les graphiques sont créés
- [ ] L'orthographe est vérifiée
- [ ] Les animations sont testées
- [ ] Le fichier est sauvegardé en .pptx

---

## 💡 Astuce : Template PowerPoint

Pour gagner du temps, vous pouvez :
1. Créer un template avec le design de base
2. Dupliquer les slides
3. Modifier uniquement le contenu

---

## 🆘 Besoin d'aide ?

Si vous avez des difficultés :
1. Utilisez l'**Option 1** (création manuelle) - c'est la plus simple
2. Suivez le guide `GUIDE_PRESENTATION_POWERPOINT.md` pour les détails
3. Le fichier MD contient tout le contenu nécessaire

---

## 📥 Une fois le PowerPoint créé

Pour le télécharger/partager :
1. **Enregistrer** : Fichier → Enregistrer sous
2. **Choisir l'emplacement** : Bureau, Documents, etc.
3. **Nommer** : `PRESENTATION_DIVALEO.pptx`
4. **Cliquer sur Enregistrer**

Le fichier sera maintenant disponible sur votre ordinateur !

