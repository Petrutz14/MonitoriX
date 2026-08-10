import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { MachineDetail } from './components/machine-detail/machine-detail';
import { LoginComponent } from './components/login/login';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
    { path: 'machine/:machineId', component: MachineDetail, canActivate: [authGuard] },
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
