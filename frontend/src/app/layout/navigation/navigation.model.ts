export type NavigationDeniedMode = 'in-shell' | 'route';

export interface AppNavigationNode {
  id: string;
  sectionId: string;
  labelKey: string;
  icon: string;
  routerLink: readonly string[];
  routePrefix: string;
  breadcrumbKey: string;
  deniedMode: NavigationDeniedMode;
}

export interface AppNavigationLeaf extends AppNavigationNode {
  children?: never;
}

export interface AppNavigationSection extends AppNavigationNode {
  children: readonly AppNavigationLeaf[];
}
