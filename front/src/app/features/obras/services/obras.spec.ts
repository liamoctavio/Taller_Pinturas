import { TestBed } from '@angular/core/testing';

import { Obras } from './obras';

describe('Obras', () => {
  let service: Obras;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Obras);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
