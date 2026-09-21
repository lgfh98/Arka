import { Injectable, signal, effect } from '@angular/core';

export type AppTheme = 'dark' | 'light';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'arka_theme';
  readonly theme = signal<AppTheme>(this.getInitialTheme());

  constructor() {
    effect(() => {
      const current = this.theme();
      if (typeof window !== 'undefined' && typeof document !== 'undefined') {
        localStorage.setItem(this.THEME_KEY, current);
        document.documentElement.setAttribute('data-theme', current);
        if (current === 'dark') {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
      }
    });
  }

  toggleTheme(): void {
    this.theme.update(current => (current === 'dark' ? 'light' : 'dark'));
  }

  private getInitialTheme(): AppTheme {
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      const saved = localStorage.getItem(this.THEME_KEY) as AppTheme | null;
      if (saved === 'light' || saved === 'dark') {
        return saved;
      }
      if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
        return 'light';
      }
    }
    return 'dark'; // Dark default for modern high-contrast enterprise look
  }
}
