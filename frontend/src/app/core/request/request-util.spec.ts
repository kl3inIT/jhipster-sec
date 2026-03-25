import { createRequestOption } from './request-util';

describe('createRequestOption', () => {
  it('appends scalar parameters', () => {
    const params = createRequestOption({ page: 1, query: 'alice' });

    expect(params.get('page')).toBe('1');
    expect(params.get('query')).toBe('alice');
  });

  it('appends array parameters one value at a time', () => {
    const params = createRequestOption({ sort: ['id,asc', 'login,desc'] });

    expect(params.getAll('sort')).toEqual(['id,asc', 'login,desc']);
  });

  it('suppresses empty string values', () => {
    const params = createRequestOption({ query: '' });

    expect(params.keys()).toEqual([]);
  });

  it('suppresses null and undefined values', () => {
    const params = createRequestOption({ query: undefined, page: null });

    expect(params.keys()).toEqual([]);
  });
});
