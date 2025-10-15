import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManTopHeaderComponent } from './man-top-header.component';

describe('ManTopHeaderComponent', () => {
  let component: ManTopHeaderComponent;
  let fixture: ComponentFixture<ManTopHeaderComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ManTopHeaderComponent]
    });
    fixture = TestBed.createComponent(ManTopHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
