import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeleteDemandeAchatComponent } from './delete-demande-achat.component';

describe('DeleteDemandeAchatComponent', () => {
  let component: DeleteDemandeAchatComponent;
  let fixture: ComponentFixture<DeleteDemandeAchatComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DeleteDemandeAchatComponent]
    });
    fixture = TestBed.createComponent(DeleteDemandeAchatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
