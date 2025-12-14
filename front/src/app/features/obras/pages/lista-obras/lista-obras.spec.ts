import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListaObras } from './lista-obras';

describe('ListaObras', () => {
  let component: ListaObras;
  let fixture: ComponentFixture<ListaObras>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListaObras]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListaObras);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
