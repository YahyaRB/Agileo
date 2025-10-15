import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffaireReceptionComponent } from './affaire-reception.component';

describe('AffaireReceptionComponent', () => {
  let component: AffaireReceptionComponent;
  let fixture: ComponentFixture<AffaireReceptionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffaireReceptionComponent]
    });
    fixture = TestBed.createComponent(AffaireReceptionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
