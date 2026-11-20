import { InjectionToken } from '@angular/core';

/**
 * Base URL of the public API. Always points at the Gateway: the Angular
 * application must never know internal service addresses.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');
