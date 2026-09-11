import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { CarteirasApiService } from '../core/api/api.services';
import { Carteira, Posicao, Venda } from '../core/api/api.models';
import { formatMoney as money } from '../shared/money.util';
import { NotificationService } from '../core/feedback/notification.service';
import { AssetLogoComponent } from '../shared/asset-logo.component';

type CurrencyFilter = 'BRL' | 'USD';
type PeriodFilter = 6 | 12 | 0;

@Component({
  standalone: true,
  imports: [FormsModule, DecimalPipe, AssetLogoComponent],
  templateUrl: './vendas.page.html',
  styleUrls: ['./vendas.page.scss', './vendas.analytics.scss']
})
export class VendasPage {
  private api=inject(CarteirasApiService); private notice=inject(NotificationService); readonly money=money;
  carteiras=signal<Carteira[]>([]); posicoes=signal<Posicao[]>([]); vendas=signal<Venda[]>([]); carteiraId=signal<number|null>(null); posicaoId=signal<number|null>(null); posicao=signal<Posicao|null>(null); previsao=signal<Venda|null>(null); salvando=signal(false);
  quantidade=0; preco=0; taxas=0; data=new Date().toISOString().slice(0,10); precoModo:'ATUAL'|'INFORMADO'='ATUAL'; currencyFilter=signal<CurrencyFilter>('USD'); periodFilter=signal<number>(12); hoveredChart=signal<string|null>(null);
  filteredVendas=computed(()=>this.vendas().filter(v=>v.moeda===this.currencyFilter())); netTotal=computed(()=>this.filteredVendas().reduce((s,v)=>s+v.resultadoRealizado,0));
  currencyTotals=computed(()=>{const result={BRL:{profit:0,loss:0,net:0},USD:{profit:0,loss:0,net:0}};for(const v of this.vendas()){const c=v.moeda==='BRL'?'BRL':'USD';result[c].net+=v.resultadoRealizado;if(v.resultadoRealizado>=0)result[c].profit+=v.resultadoRealizado;else result[c].loss+=v.resultadoRealizado;}return result;});
  chartData=computed(()=>{const rows=new Map<string,{value:number,count:number}>();for(const v of this.filteredVendas()){const key=v.dataVenda.slice(0,7),old=rows.get(key);rows.set(key,{value:(old?.value??0)+v.resultadoRealizado,count:(old?.count??0)+1});}let values=[...rows.entries()].sort(([a],[b])=>a.localeCompare(b));const period=this.periodFilter();if(period)values=values.slice(-period);const max=Math.max(1,...values.map(([,x])=>Math.abs(x.value)));return values.map(([key,x])=>({key,label:key.slice(5,7)+'/'+key.slice(2,4),monthName:new Intl.DateTimeFormat('pt-BR',{month:'long',year:'numeric'}).format(new Date(`${key}-01T12:00:00`)),value:x.value,count:x.count,height:Math.max(4,Math.abs(x.value)/max*100)}));});
  selectedTotals=computed(()=>this.currencyTotals()[this.currencyFilter()]);
  metricMoney(kind:'profit'|'loss'|'net'):string{const value=this.selectedTotals()[kind];return kind==='loss'&&value<0?`-${money(Math.abs(value),this.currencyFilter())}`:money(Math.abs(value),this.currencyFilter());}
  constructor(){this.api.list(0,100).subscribe(r=>{this.carteiras.set(r.content);if(!r.content.length)return;forkJoin(r.content.map(c=>this.api.listSales(c.id))).subscribe(series=>{const indice=series.findIndex(v=>v.length>0);this.carregar(r.content[indice>=0?indice:0].id);});});}
  carregar(id:number|null){this.carteiraId.set(id);this.posicaoId.set(null);this.posicao.set(null);this.previsao.set(null);if(id){this.api.listPositions(id).subscribe(p=>this.posicoes.set(p.filter(x=>x.quantidade>0)));this.api.listSales(id).subscribe(v=>{this.vendas.set(v);const moedas=[...new Set(v.map(x=>x.moeda).filter(x=>x==='BRL'||x==='USD'))] as CurrencyFilter[];if(moedas.length===1)this.currencyFilter.set(moedas[0]);else if(moedas.length>1&&!moedas.includes(this.currencyFilter()))this.currencyFilter.set(moedas[0]);});}else{this.posicoes.set([]);this.vendas.set([]);this.currencyFilter.set('BRL');}}
  selecionar(id:number|null){this.posicaoId.set(id);const p=this.posicoes().find(x=>x.id===id)||null;this.posicao.set(p);if(p){this.preco=p.cotacaoAtual??p.precoMedio;this.precoModo='ATUAL';this.quantidade=0;this.taxas=0;this.simular();}else this.previsao.set(null);}
  usarCotacaoAtual(){const p=this.posicao();this.precoModo='ATUAL';if(p)this.preco=p.cotacaoAtual??p.precoMedio;this.simular();} usarPrecoInformado(){this.precoModo='INFORMADO';this.simular();}
  valido(){const p=this.posicao();return!!p&&this.quantidade>0&&this.quantidade<=p.quantidade&&this.preco>0&&this.taxas>=0&&!!this.data;}
  simular(){const p=this.posicao();if(!p||this.quantidade<=0||this.preco<=0||this.taxas<0){this.previsao.set(null);return;}const bruto=this.quantidade*this.preco,custo=this.quantidade*p.precoMedio,resultado=bruto-custo-this.taxas;this.previsao.set({id:0,carteiraId:this.carteiraId()!,posicaoId:p.id,ticker:p.ticker,nomeEmpresa:p.nomeEmpresa,quantidade:this.quantidade,precoMedio:p.precoMedio,precoVenda:this.preco,taxas:this.taxas,valorBruto:bruto,custoPosicao:custo,resultadoRealizado:resultado,rentabilidadePercentual:custo?resultado/custo*100:0,moeda:p.moeda??'BRL',dataVenda:this.data});}
  confirmar(){const id=this.carteiraId(),p=this.posicao();if(!id||!p||!this.valido()||this.salvando())return;this.salvando.set(true);this.api.sell(id,{posicaoId:p.id,quantidade:this.quantidade,precoVenda:this.preco,taxas:this.taxas,dataVenda:this.data,moeda:p.moeda??undefined}).pipe(finalize(()=>this.salvando.set(false))).subscribe({next:()=>{this.notice.show('Venda confirmada.','success');this.carregar(id);},error:e=>this.notice.show(e?.error?.message??'Falha ao confirmar venda.','error')});}
}
