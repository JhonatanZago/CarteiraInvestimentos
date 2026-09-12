import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NotificationService } from '../core/feedback/notification.service';
import { ThemeService } from '../core/theme/theme.service';
import { AuthService } from '../core/auth/auth.service';

@Component({selector:'app-app-layout',standalone:true,imports:[RouterLink,RouterLinkActive,RouterOutlet],templateUrl:'./app-layout.component.html',styleUrl:'./app-layout.component.scss'})
export class AppLayoutComponent {
  readonly menuOpen=signal(false); readonly accountOpen=signal(false);
  readonly notifications=inject(NotificationService); readonly themeService=inject(ThemeService); readonly auth=inject(AuthService); private readonly router=inject(Router);
  initials(name:string,email:string):string { const source=(name||'').trim() || (email||'').split('@')[0]; const parts=source.split(/\s+/).filter(Boolean); return (parts.length>1 ? parts[0][0]+parts[parts.length-1][0] : source.slice(0,2)).toUpperCase(); }
  toggleAccount():void { this.accountOpen.update(v=>!v); }
  closeMenus():void { this.accountOpen.set(false); this.menuOpen.set(false); }
  switchAccount():void { this.closeMenus(); this.auth.logout().subscribe(()=>this.router.navigateByUrl('/login',{replaceUrl:true})); }
  @HostListener('document:keydown.escape') onEscape():void { this.closeMenus(); }
  @HostListener('document:click',['$event']) onDocumentClick(event:MouseEvent):void { const target=event.target as HTMLElement; if(!target.closest('.account-control')) this.accountOpen.set(false); }
}
