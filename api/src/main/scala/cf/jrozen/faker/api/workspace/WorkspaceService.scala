package cf.jrozen.faker.api.workspace

import cats.data.EitherT
import cats.{Functor, Monad}
import cf.jrozen.faker.api.{WorkspaceAlreadyExistsError, WorkspaceNotFoundError}
import cf.jrozen.faker.model.{UUID, Workspace}
import cf.jrozen.faker.mongo.repository.WorkspaceRepository

class WorkspaceService[F[_] : Monad : Functor](workspaceRepository: WorkspaceRepository[F], workspaceValidationAlgebra: WorkspaceValidationAlgebra[F]) {

  def addWorkspace(wsRequest: WorkspaceRequest): EitherT[F, WorkspaceAlreadyExistsError, Workspace] = {
    workspaceValidationAlgebra.doesNotExist(wsRequest).flatMap { _ =>
      val workspace = createWorkspace(wsRequest)
      EitherT.liftF(workspaceRepository.save(workspace)).map(_ => workspace)
    }
  }

  def getWorkspace(workspaceUuid: UUID): EitherT[F, WorkspaceNotFoundError.type, Workspace] =
    EitherT.fromOptionF(workspaceRepository.findByUuid(workspaceUuid), WorkspaceNotFoundError)

  def deleteWorkspace(workspaceUuid: UUID): EitherT[F, WorkspaceNotFoundError.type, Workspace] =
    getWorkspace(workspaceUuid).flatMap { ws =>
      EitherT.liftF(workspaceRepository.deleteByUuid(workspaceUuid)).map(_ => ws)
    }

  def createWorkspace(wsRequest: WorkspaceRequest): Workspace = Workspace(name = wsRequest.name)

}

object WorkspaceService {
  def apply[F[_] : Monad](workspacesRepository: WorkspaceRepository[F], workspaceValidationAlgebra: WorkspaceValidationAlgebra[F]): WorkspaceService[F] =
    new WorkspaceService[F](workspacesRepository, workspaceValidationAlgebra)
}