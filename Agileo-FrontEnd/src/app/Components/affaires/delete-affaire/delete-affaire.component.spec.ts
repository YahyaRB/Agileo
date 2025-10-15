import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeleteAffaireComponent } from './delete-affaire.component';

describe('DeleteAffaireComponent', () => {
  let component: DeleteAffaireComponent;
  let fixture: ComponentFixture<DeleteAffaireComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DeleteAffaireComponent]
    });
    fixture = TestBed.createComponent(DeleteAffaireComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
