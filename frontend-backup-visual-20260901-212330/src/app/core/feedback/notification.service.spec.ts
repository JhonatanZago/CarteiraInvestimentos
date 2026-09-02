import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  it('does not add duplicate visible message and tone pairs', () => {
    const service = TestBed.inject(NotificationService);
    service.show('Falha de rede', 'error'); service.show('Falha de rede', 'error');
    expect(service.notifications()).toHaveLength(1);
  });
});
