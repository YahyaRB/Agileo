import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffectationUserAccessComponent } from './affectation-user-access.component';

describe('AffectationUserAccessComponent', () => {
  let component: AffectationUserAccessComponent;
  let fixture: ComponentFixture<AffectationUserAccessComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffectationUserAccessComponent]
    });
    fixture = TestBed.createComponent(AffectationUserAccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
