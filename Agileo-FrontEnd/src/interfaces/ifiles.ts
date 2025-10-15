export interface IFiles {
  file: File;
  name: string;
  generatedName?: string;
  fileId?: number;
  size?: number;
  sizeFormatted?: string;
  uploadDate?: string;
  canDelete?: boolean;
}
