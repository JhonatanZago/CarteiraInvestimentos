import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { CarteirasApiService } from '../core/api/api.services';
import { Carteira, Posicao, Venda } from '../core/api/api.models';
import { formatMoney as money } from '../shared/money.util';
import { NotificationService } from '../core/feedback/notification.service';
import { AssetLogoComponent } from '../shared/asset-logo.component';

@Component({
  standalone: true,
  imports: [FormsModule, AssetLogoComponent],
  template: `<section class="page"><header class="page-header"><div><h1>Central de vendas</h1><p class="muted">Venda parcial ou total de uma posicao.</p></div></header>
  <section class="panel"><label>Carteira <select [ngModel]="carteiraId()" (ngModelChange)="carregar($event)"><option [ngValue]="null">Selecione</option>@for(c of carteiras();track c.id){<option [ngValue]="c.id">{{c.nome}}</option>}</select></label>
  <label>Posicao <select [ngModel]="posicaoId()" (ngModelChange)="selecionar($event)" [disabled]="!carteiraId()"><option [ngValue]="null">Selecione uma posicao com saldo</option>@for(p of posicoes();track p.id){<option [ngValue]="p.id">{{p.ticker}} - {{p.quantidade}}</option>}</select></label>
  @if(posicao();as p){<p><app-asset-logo [ticker]="p.ticker" [companyName]="p.nomeEmpresa" [logoUrl]="p.logoUrl" [size]="40"/><strong>{{p.ticker}} - {{p.nomeEmpresa}}</strong> | Disponivel: {{p.quantidade}} | Preco medio: {{money(p.precoMedio,p.moeda)}}</p><div class="form-grid"><label>Preco de venda<input type="number" min="0.000001" [(ngModel)]="preco" (ngModelChange)="simular()"></label><label>Quantidade<input type="number" min="0.000001" [(ngModel)]="quantidade" (ngModelChange)="simular()"></label><label>Taxas<input type="number" min="0" [(ngModel)]="taxas" (ngModelChange)="simular()"></label><label>Data<input type="date" [(ngModel)]="data"></label></div><button type="button" [disabled]="salvando() || !valido()" (click)="confirmar()">{{salvando()?'Confirmando...':'Confirmar venda'}}</button>}
  @if(previsao();as s){<section class="panel"><h2>Resumo da operacao</h2><p>Bruto: {{money(s.valorBruto,s.moeda)}} | Custo: {{money(s.custoPosicao,s.moeda)}} | Taxas: {{money(s.taxas,s.moeda)}}</p><p>Resultado: {{money(s.resultadoRealizado,s.moeda)}} | Restante: {{s.quantidadeRestante}}</p></section>}</section>
  <section class="panel"><h2>Historico de vendas</h2>@if(!vendas().length){<p class="muted">Nenhuma venda confirmada.</p>}@else{<div class="table-scroll"><table><thead><tr><th>Data</th><th>Ativo</th><th>Quantidade</th><th>Preco medio</th><th>Preco venda</th><th>Taxas</th><th>Resultado</th></tr></thead><tbody>@for(v of vendas();track v.id){<tr><td>{{v.dataVenda}}</td><td>{{v.ticker}}</td><td>{{v.quantidade}}</td><td>{{money(v.precoMedio,v.moeda)}}</td><td>{{money(v.precoVenda,v.moeda)}}</td><td>{{money(v.taxas,v.moeda)}}</td><td>{{money(v.resultadoRealizado,v.moeda)}}</td></tr>}</tbody></table></div>}</section></section>`
})
export class VendasPage {
  private api=inject(CarteirasApiService); private notice=inject(NotificationService); readonly money=money;
  carteiras=signal<Carteira[]>([]); posicoes=signal<Posicao[]>([]); vendas=signal<Venda[]>([]); carteiraId=signal<number|null>(null); posicaoId=signal<number|null>(null); posicao=signal<Posicao|null>(null); previsao=signal<Venda|null>(null); salvando=signal(false); quantidade=0; preco=0; taxas=0; data=new Date().toISOString().slice(0,10);
  constructor(){this.api.list(0,100).subscribe(r=>this.carteiras.set(r.content));}
  carregar(id:number|null){this.carteiraId.set(id);this.posicaoId.set(null);this.posicao.set(null);this.previsao.set(null);if(id){this.api.listPositions(id).subscribe(p=>this.posicoes.set(p.filter(x=>x.quantidade>0)));this.api.listSales(id).subscribe(v=>this.vendas.set(v));}else{this.posicoes.set([]);this.vendas.set([]);}}
  selecionar(id:number|null){this.posicaoId.set(id);const p=this.posicoes().find(x=>x.id===id)||null;this.posicao.set(p);if(p){this.preco=p.cotacaoAtual??p.precoMedio;this.quantidade=0;this.taxas=0;this.simular();}else this.previsao.set(null);}
  valido(){const p=this.posicao();return !!p&&this.quantidade>0&&this.quantidade<=p.quantidade&&this.preco>0&&this.taxas>=0&&!!this.data;}
  simular(){const p=this.posicao();if(!p||this.quantidade<=0||this.preco<=0||this.taxas<0){this.previsao.set(null);return;}const bruto=this.quantidade*this.preco,custo=this.quantidade*p.precoMedio,resultado=bruto-custo-this.taxas;this.previsao.set({id:0,carteiraId:this.carteiraId()!,posicaoId:p.id,ticker:p.ticker,nomeEmpresa:p.nomeEmpresa,quantidade:this.quantidade,precoMedio:p.precoMedio,precoVenda:this.preco,taxas:this.taxas,valorBruto:bruto,custoPosicao:custo,resultadoRealizado:resultado,rentabilidadePercentual:custo?resultado/custo*100:0,moeda:p.moeda??'BRL',dataVenda:this.data});}
  confirmar(){const id=this.carteiraId(),p=this.posicao();if(!id||!p||!this.valido()||this.salvando())return;this.salvando.set(true);this.api.sell(id,{posicaoId:p.id,quantidade:this.quantidade,precoVenda:this.preco,taxas:this.taxas,dataVenda:this.data,moeda:p.moeda??undefined}).pipe(finalize(()=>this.salvando.set(false))).subscribe({next:()=>{this.notice.show('Venda confirmada.','success');this.carregar(id);},error:e=>this.notice.show(e?.error?.message??'Falha ao confirmar venda.','error')});}
}
