import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { ToolbarComponent } from './layout/toolbar/toolbar.component';
import { SidebarComponent } from './layout/sidebar/sidebar.component';
import { ThemeService } from './services/theme.service';
import { KeyboardShortcutsService } from './services/keyboard-shortcuts.service';
import { routeAnimations } from './animations';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatSidenavModule, ToolbarComponent, SidebarComponent],
  animations: [routeAnimations],
  template: `
    <div class="app-layout">
      <app-toolbar />
      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav mode="side" opened class="sidenav">
          <app-sidebar />
        </mat-sidenav>
        <mat-sidenav-content class="main-content">
          <div [@routeAnimations]="outlet.activatedRouteData['animation']">
            <router-outlet #outlet="outlet" />
          </div>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private themeService = inject(ThemeService);
  // Injecter pour initialiser l'ecoute des raccourcis clavier
  private _shortcuts = inject(KeyboardShortcutsService);

  ngOnInit(): void {
    this.themeService.apply();
  }
}
