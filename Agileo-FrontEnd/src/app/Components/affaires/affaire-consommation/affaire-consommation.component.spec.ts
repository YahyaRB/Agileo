import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffaireConsommationComponent } from './affaire-consommation.component';

describe('AffaireConsommationComponent', () => {
  let component: AffaireConsommationComponent;
  let fixture: ComponentFixture<AffaireConsommationComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffaireConsommationComponent]
    });
    fixture = TestBed.createComponent(AffaireConsommationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
