import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { ISecCatalogEntry } from '../shared/sec-catalog.model';
import { ISecPermission } from '../shared/sec-permission.model';
import { SecCatalogService } from '../shared/service/sec-catalog.service';
import { SecPermissionService } from '../shared/service/sec-permission.service';

interface AttributeRow {
  label: string;
  target: string;
  isWildcard: boolean;
}

@Component({
  selector: 'app-permission-matrix',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ButtonModule, CheckboxModule, ProgressSpinnerModule, SplitterModule, TableModule, ToastModule],
  providers: [MessageService],
  templateUrl: './permission-matrix.component.html',
})
export default class PermissionMatrixComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(SecCatalogService);
  private readonly permissionService = inject(SecPermissionService);
  private readonly messageService = inject(MessageService);
  private readonly cdr = inject(ChangeDetectorRef);

  roleName = '';
  catalogEntries: ISecCatalogEntry[] = [];
  loading = true;
  selectedEntity: ISecCatalogEntry | null = null;

  granted = new Map<string, number>();
  pending = new Set<string>();

  ngOnInit(): void {
    this.roleName = this.route.snapshot.paramMap.get('name') ?? '';
    if (!this.roleName) {
      this.loading = false;
      return;
    }

    forkJoin({
      catalogEntries: this.catalogService.query(),
      permissions: this.permissionService.query(this.roleName),
    })
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: ({ catalogEntries, permissions }) => {
          this.catalogEntries = catalogEntries;
          this.granted.clear();
          permissions.forEach(permission => {
            if (permission.id !== undefined) {
              this.granted.set(this.permissionKey(permission.target, permission.action), permission.id);
            }
          });
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Could not load the permission matrix. Please try again.',
          });
        },
      });
  }

  onEntitySelect(entry: ISecCatalogEntry): void {
    this.selectedEntity = entry;
    this.cdr.detectChanges();
  }

  isGranted(target: string, action: string): boolean {
    return this.granted.has(this.permissionKey(target, action));
  }

  isWildcardGranted(entityCode: string, action: string): boolean {
    return this.isGranted(`${entityCode}.*`, action);
  }

  isPending(target: string, action: string): boolean {
    return this.pending.has(this.permissionKey(target, action));
  }

  getAttributeRows(entity: ISecCatalogEntry): AttributeRow[] {
    return [
      { label: 'All attributes (*)', target: `${entity.code}.*`, isWildcard: true },
      ...entity.attributes.map(attribute => ({
        label: attribute,
        target: `${entity.code}.${attribute}`,
        isWildcard: false,
      })),
    ];
  }

  togglePermission(targetType: string, target: string, action: string, checked: boolean): void {
    const key = this.permissionKey(target, action);
    if (this.pending.has(key)) {
      return;
    }

    this.pending.add(key);

    if (checked) {
      this.granted.set(key, -1);
      this.cdr.detectChanges();
      const permission: ISecPermission = {
        authorityName: this.roleName,
        targetType,
        target,
        action,
        effect: 'GRANT',
      };

      this.permissionService.create(permission).subscribe({
        next: response => {
          const permissionId = response.body?.id;
          if (permissionId !== undefined) {
            this.granted.set(key, permissionId);
          } else {
            this.granted.delete(key);
          }
          this.pending.delete(key);
          this.cdr.detectChanges();
        },
        error: () => {
          this.granted.delete(key);
          this.pending.delete(key);
          this.showSaveError();
          this.cdr.detectChanges();
        },
      });
      return;
    }

    const permissionId = this.granted.get(key);
    if (permissionId === undefined || permissionId < 0) {
      this.pending.delete(key);
      return;
    }

    this.granted.delete(key);
    this.cdr.detectChanges();
    this.permissionService.delete(permissionId).subscribe({
      next: () => {
        this.pending.delete(key);
        this.cdr.detectChanges();
      },
      error: () => {
        this.granted.set(key, permissionId);
        this.pending.delete(key);
        this.showSaveError();
        this.cdr.detectChanges();
      },
    });
  }

  private permissionKey(target: string, action: string): string {
    return `${target}:${action}`;
  }

  private showSaveError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Save failed',
      detail: 'Could not update the permission. Please try again.',
    });
  }
}
