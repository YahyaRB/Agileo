import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffectationUserAffaireComponent } from './affectation-user-affaire.component';

describe('AffectationUserAffaireComponent', () => {
  let component: AffectationUserAffaireComponent;
  let fixture: ComponentFixture<AffectationUserAffaireComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffectationUserAffaireComponent]
    });
    fixture = TestBed.createComponent(AffectationUserAffaireComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
