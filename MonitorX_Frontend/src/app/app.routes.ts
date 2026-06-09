import { Routes } from '@angular/router';
import { Dashboard } from './components/dashboard/dashboard';
import { MachineDetail } from './components/machine-detail/machine-detail';

export const routes: Routes = [
    { path: 'dashboard', component: Dashboard },
    { path: 'machine/:machineId', component: MachineDetail },
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
