import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/theme/theme.service';

@Component({selector:'app-login-page',standalone:true,imports:[ReactiveFormsModule,RouterLink],templateUrl:'./login.page.html',styleUrl:'./login.page.scss'})
export class LoginPage {
  private readonly fb=inject(FormBuilder); private readonly auth=inject(AuthService); private readonly router=inject(Router); readonly theme=inject(ThemeService); readonly loading=this.auth.loading; error='';
  readonly form=this.fb.nonNullable.group({email:['',[Validators.required,Validators.email]],senha:['',Validators.required],manterConectado:[true]}); showPassword=false;
  submit():void { if(this.form.invalid||this.loading()) {this.form.markAllAsTouched();return;} this.error=''; this.auth.login(this.form.getRawValue()).subscribe({next:()=>this.router.navigateByUrl('/visao-geral'),error:e=>this.error=e?.error?.message||'E-mail ou senha inválidos.'}); }
}
