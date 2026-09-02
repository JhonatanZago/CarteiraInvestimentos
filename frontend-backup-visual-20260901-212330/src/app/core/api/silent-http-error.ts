import { HttpContextToken } from '@angular/common/http';

/** Marks requests whose screen renders its own recovery state. */
export const SILENT_HTTP_ERROR = new HttpContextToken<boolean>(() => false);
