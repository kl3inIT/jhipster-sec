import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TreeModule } from 'primeng/tree';
import { ConfirmationService, MessageService, TreeNode } from 'primeng/api';

import { ISecMenuDefinition, ISyncNode } from '../sec-menu-definition.model';
import { SecMenuDefinitionService } from '../service/sec-menu-definition.service';
import MenuDefinitionDialogComponent from '../dialog/menu-definition-dialog.component';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { APP_NAVIGATION_TREE } from 'app/layout/navigation/navigation-registry';

@Component({
  selector: 'app-menu-definition-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    ButtonModule,
    CardModule,
    ToastModule,
    ConfirmDialogModule,
    TreeModule,
    MenuDefinitionDialogComponent,
    TranslatePipe,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './menu-definition-list.component.html',
})
export default class MenuDefinitionListComponent implements OnInit {
  definitions: ISecMenuDefinition[] = [];
  treeNodes: TreeNode[] = [];
  isLoading = false;
  isSyncing = false;
  dialogVisible = false;
  selectedDefinition: ISecMenuDefinition | null = null;

  private readonly service = inject(SecMenuDefinitionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadDefinitions();
  }

  loadDefinitions(): void {
    this.isLoading = true;
    this.service
      .query()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: response => {
          this.definitions = response.body ?? [];
          this.buildTree();
        },
        error: (err: unknown) =>
          handleHttpError(this.messageService, this.translateService, err, 'feedback.security.menuDefinitions.loadFailed'),
      });
  }

  private buildTree(): void {
    const nodeMap = new Map<string, TreeNode>();
    for (const def of this.definitions) {
      nodeMap.set(def.menuId, { key: String(def.id), label: def.label, data: def, expanded: true, children: [] });
    }
    const roots: TreeNode[] = [];
    for (const def of this.definitions) {
      const node = nodeMap.get(def.menuId)!;
      if (def.parentMenuId && nodeMap.has(def.parentMenuId)) {
        nodeMap.get(def.parentMenuId)!.children!.push(node);
      } else {
        roots.push(node);
      }
    }
    const sortNodes = (nodes: TreeNode[]) => {
      nodes.sort((a, b) => ((a.data as ISecMenuDefinition).ordering ?? 0) - ((b.data as ISecMenuDefinition).ordering ?? 0));
      nodes.forEach(n => { if (n.children?.length) sortNodes(n.children); });
    };
    sortNodes(roots);
    this.treeNodes = roots;
  }

  openCreate(): void {
    this.selectedDefinition = null;
    this.dialogVisible = true;
    this.cdr.markForCheck();
  }

  openEdit(def: ISecMenuDefinition): void {
    this.selectedDefinition = { ...def };
    this.dialogVisible = true;
    this.cdr.markForCheck();
  }

  onSaved(): void {
    this.dialogVisible = false;
    this.loadDefinitions();
  }

  onDialogClosed(): void {
    this.dialogVisible = false;
    this.cdr.markForCheck();
  }

  confirmDelete(def: ISecMenuDefinition): void {
    this.confirmationService.confirm({
      header: 'Delete Menu Definition',
      message: 'Are you sure you want to delete this menu definition? Role permissions for this menu node will also be removed.',
      acceptLabel: 'Delete Definition',
      rejectLabel: 'Keep Definition',
      defaultFocus: 'reject',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deleteDefinition(def),
    });
  }

  private deleteDefinition(def: ISecMenuDefinition): void {
    this.service.delete(def.id!).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.security.menuDefinitions.deleted',
          detailParams: { param: def.menuId },
        });
        this.loadDefinitions();
      },
      error: (err: unknown) =>
        handleHttpError(this.messageService, this.translateService, err, 'feedback.security.menuDefinitions.deleteFailed'),
    });
  }

  confirmSync(): void {
    this.confirmationService.confirm({
      header: 'Sync from Registry',
      message:
        'This will add any navigation nodes from the frontend registry that are not yet in the catalog. Existing definitions will not be modified.',
      acceptLabel: 'Sync Now',
      rejectLabel: 'Keep Current Catalog',
      accept: () => this.syncFromRegistry(),
    });
  }

  private syncFromRegistry(): void {
    const nodes: ISyncNode[] = [];
    let sectionIndex = 0;
    for (const section of APP_NAVIGATION_TREE) {
      nodes.push({
        menuId: section.id,
        appName: 'jhipster-security-platform',
        menuName: section.id,
        label: section.labelKey,
        parentMenuId: undefined,
        route: section.routerLink[0],
        icon: section.icon,
        ordering: sectionIndex,
      });
      let childIndex = 0;
      for (const child of section.children) {
        nodes.push({
          menuId: child.id,
          appName: 'jhipster-security-platform',
          menuName: child.id,
          label: child.labelKey,
          parentMenuId: section.id,
          route: child.routerLink[0],
          icon: child.icon,
          ordering: childIndex,
        });
        childIndex++;
      }
      sectionIndex++;
    }

    this.isSyncing = true;
    this.cdr.markForCheck();

    this.service
      .sync(nodes)
      .pipe(
        finalize(() => {
          this.isSyncing = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: result => {
          addTranslatedMessage(this.messageService, this.translateService, {
            severity: 'success',
            summary: 'feedback.toast.deleted',
            detail: 'feedback.security.menuDefinitions.syncSuccess',
            detailParams: { seeded: result.body?.seeded ?? 0 },
          });
          this.loadDefinitions();
        },
        error: (err: unknown) =>
          handleHttpError(this.messageService, this.translateService, err, 'feedback.security.menuDefinitions.syncFailed'),
      });
  }
}
