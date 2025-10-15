import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffaireDemandeAchatComponent } from './affaire-demande-achat.component';

describe('AffaireDemandeAchatComponent', () => {
  let component: AffaireDemandeAchatComponent;
  let fixture: ComponentFixture<AffaireDemandeAchatComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffaireDemandeAchatComponent]
    });
    fixture = TestBed.createComponent(AffaireDemandeAchatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
