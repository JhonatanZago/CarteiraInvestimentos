import { Component } from '@angular/core'; import { RouterOutlet } from '@angular/router';
@Component({selector:'app-auth-layout',standalone:true,imports:[RouterOutlet],template:`<main class="auth-layout"><router-outlet /></main>`,styles:[`.auth-layout{display:block;width:100%;min-height:100dvh;overflow-x:hidden}`]})
export class AuthLayoutComponent {}
