import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MachineResponse } from '../models/machine-response.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MachineService {
    private apiUrl = `${environment.apiUrl}/api/machines`;

    constructor(private http: HttpClient) {}

    getMachines(): Observable<MachineResponse[]> {
        return this.http.get<MachineResponse[]>(this.apiUrl);
    }

    getMachine(machineId: string): Observable<MachineResponse> {
        return this.http.get<MachineResponse>(`${this.apiUrl}/${machineId}`);
    }

    updateDisplayName(machineId: string, displayName: string): Observable<MachineResponse> {
        return this.http.patch<MachineResponse>(`${this.apiUrl}/${machineId}`, { displayName });
    }
}
