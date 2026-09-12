import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/theme/theme.service';

@Component({selector:'app-register-page',standalone:true,imports:[ReactiveFormsModule,RouterLink],templateUrl:'./register.page.html',styleUrl:'./register.page.scss'})
export class RegisterPage { private readonly fb=inject(FormBuilder); private readonly auth=inject(AuthService); private readonly router=inject(Router); readonly theme=inject(ThemeService); readonly loading=this.auth.loading; readonly form=this.fb.nonNullable.group({nome:['',Validators.required],email:['',[Validators.required,Validators.email]],senha:['',Validators.required],confirmacaoSenha:['',Validators.required]}); error=''; success=''; showPassword=false; showConfirmation=false; submit():void { if(this.form.invalid||this.loading()){this.form.markAllAsTouched();return;} if(this.form.controls.senha.value!==this.form.controls.confirmacaoSenha.value){this.error='As senhas não coincidem.';return;} this.error=''; this.auth.register(this.form.getRawValue()).subscribe({next:()=>{this.success='Conta criada com sucesso.'; void this.router.navigateByUrl('/login');},error:e=>this.error=e?.error?.message||'Não foi possível criar sua conta.'}); } }
