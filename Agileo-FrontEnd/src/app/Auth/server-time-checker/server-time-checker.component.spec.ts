import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServerTimeCheckerComponent } from './server-time-checker.component';

describe('ServerTimeCheckerComponent', () => {
  let component: ServerTimeCheckerComponent;
  let fixture: ComponentFixture<ServerTimeCheckerComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ServerTimeCheckerComponent]
    });
    fixture = TestBed.createComponent(ServerTimeCheckerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
