import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {WorkspaceComponent} from "./workspace/workspace.component";
import {AboutComponent} from "./about/about.component";
import {EndpointDetailsComponent} from "./workspace/endpoint-details/endpoint-details.component";
import {WorkspaceSelectComponent} from './workspace-select/workspace-select.component';

const routes: Routes = [
  {
    path: '', redirectTo: '/workspace', pathMatch: 'full'
  }, {
    path: 'workspace',
    component: WorkspaceSelectComponent,// pathMatch: 'full',
  }, {
    path: 'workspace/:workspaceName',
    component: WorkspaceComponent,// pathMatch: 'full',
    children: [
      {path: ':endpointId', component: EndpointDetailsComponent},
    ]
  }, {
    path: 'about',
    component: AboutComponent,
    pathMatch: 'full',
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
