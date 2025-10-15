import { TestBed } from '@angular/core/testing';

import { SidebarFixServiceService } from './sidebar-fix-service.service';

describe('SidebarFixServiceService', () => {
  let service: SidebarFixServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SidebarFixServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
