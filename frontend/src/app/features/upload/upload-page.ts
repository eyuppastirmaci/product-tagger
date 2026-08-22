import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideUpload } from '@lucide/angular';
import { StatusBadge } from '../../shared/ui/status-badge';
import { UploadManager } from './upload-manager';

const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

@Component({
  selector: 'app-upload-page',
  imports: [RouterLink, TranslocoPipe, StatusBadge, LucideUpload],
  templateUrl: './upload-page.html',
  styleUrl: './upload-page.scss',
})
export class UploadPage {
  protected readonly manager = inject(UploadManager);

  protected readonly dragOver = signal(false);

  protected onBrowse(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.uploadFiles(input.files);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  protected onDragLeave(): void {
    this.dragOver.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    this.uploadFiles(event.dataTransfer?.files ?? null);
  }

  private uploadFiles(files: FileList | null): void {
    if (!files) {
      return;
    }

    const accepted = Array.from(files).filter((file) => ACCEPTED_TYPES.includes(file.type));

    if (accepted.length > 0) {
      this.manager.uploadAll(accepted);
    }
  }
}
