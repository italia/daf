package it.gov.daf.securitymanager.service

import cats.data.EitherT
import play.api.Logger
import security_manager.yaml.{Error, Success}

import scala.concurrent.Future
import scala.util.Try
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class SuccessWrapper(success:Success, steps:Int )
case class ErrorWrapper(error:Error, steps:Int)


object ProcessHandler {

  private val logger = Logger("it.gov.daf.securitymanager.service.ProcessHandler")

/*
  def step( tryresp:Try[Future[Either[Error,Success]]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = step(SuccessWrapper(Success(None,None),0),tryresp)

  def step( success:SuccessWrapper, tryresp:Try[Future[Either[Error,Success]]] ):EitherT[Future,ErrorWrapper,SuccessWrapper]={

    val out:Future[Either[ErrorWrapper,SuccessWrapper]] = tryresp match{
      case scala.util.Success(resp) => resp.map{
        case Left(l) => Left( ErrorWrapper(l,success.steps) )
        case Right(r) =>  Right( SuccessWrapper(r,success.steps+1) )
      }
      case scala.util.Failure(f) => Future{Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
    }

    EitherT(out)

  }


  def step( resp:Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = step(SuccessWrapper(Success(None,None),0),resp)

  def step( success:SuccessWrapper, resp:Future[Either[Error,Success]] ):EitherT[Future,ErrorWrapper,SuccessWrapper]={

    val out:Future[Either[ErrorWrapper,SuccessWrapper]] =  resp.map{
        case Left(l) => Left( ErrorWrapper(l,success.steps) )
        case Right(r) =>  Right( SuccessWrapper(r,success.steps+1) )
      }

    EitherT(out)

  }*/




  def step(tryresp: => Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper]  = step(SuccessWrapper(Success(None,None),0),tryresp)

  def step(success:SuccessWrapper, fx: => Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = {

    handleTries(success, fx ){a:Success => val  newStep=success.steps+1
                                                Right(SuccessWrapper(a, success.steps+1))}

    /*
    val out:Future[Either[ErrorWrapper,SuccessWrapper]] =
      Try{
        fx.map{
          case Left(l) => scala.util.Success(Left(ErrorWrapper(l, success.steps)))
          case Right(r) => scala.util.Success(Right(SuccessWrapper(r, success.steps+1)))
        }.recover{case e=> scala.util.Failure(e)} map {
          case scala.util.Success(resp) => resp
          case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )
        }
      }match {
        case scala.util.Success(resp) => resp
        case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Future.successful{ Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
      }

    EitherT(out)*/
  }


  def stepOver(tryresp: => Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper]  = stepOver(SuccessWrapper(Success(None,None),0),tryresp)

  def stepOver(success:SuccessWrapper, fx: => Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = {

    handleTries(success, fx ){a:Success => Right(SuccessWrapper(a, success.steps))}
    /*
    val out:Future[Either[ErrorWrapper,SuccessWrapper]] =
      Try{
        fx.map{
          case Left(l) => scala.util.Success(Left(ErrorWrapper(l, success.steps)))
          case Right(r) => scala.util.Success(Right(SuccessWrapper(r, success.steps)))
        }.recover{case e=> scala.util.Failure(e)} map {
          case scala.util.Success(resp) => resp
          case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )
        }
      }match {
        case scala.util.Success(resp) => resp
        case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Future.successful{ Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
      }

    EitherT(out)
    */
  }

  def stepOverF[S](tryresp: => Future[Either[Error,S]] ): EitherT[Future,ErrorWrapper,S]  = stepOverF(SuccessWrapper(Success(None,None),0),tryresp)

  def stepOverF[S](success:SuccessWrapper, fx: => Future[Either[Error,S]] ): EitherT[Future,ErrorWrapper,S] = {

    handleTries(success, fx ){Right(_)}
    /*
    val out:Future[Either[ErrorWrapper,S]] =
      Try{
        fx.map{
          case Left(l) => scala.util.Success(Left(ErrorWrapper(l, success.steps)))
          case Right(r) => scala.util.Success(Right(r))
        }.recover{case e=> scala.util.Failure(e)} map {
          case scala.util.Success(resp) => resp
          case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )
        }
      }match {
        case scala.util.Success(resp) => resp
        case scala.util.Failure(f) => Logger.logger.error(f.getMessage,f);Future.successful{ Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
      }

    EitherT(out)*/
  }


  private def handleTries[S,T](success:SuccessWrapper, fx: => Future[Either[Error,S]])( ff: S=>Right[ErrorWrapper,T] ): EitherT[Future,ErrorWrapper,T] = {


    logger.debug("toDeleteTries1 "+success.steps)

    val out:Future[Either[ErrorWrapper,T]] =
      Try{
        fx.map{
          case Left(l) => val err = ErrorWrapper(l, success.steps)
                                    logger.warn(err.toString)
                                    scala.util.Success(Left(err))

          case Right(r) => scala.util.Success( ff(r) )

        }.recover{case e:Throwable=> scala.util.Failure(e)} map {
          case scala.util.Success(resp) => resp
          case scala.util.Failure(f) => logger.error(s"Future Failure:${f.getMessage}",f);logger.debug("toDeleteTries2 "+success.steps)
                                        Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )
        }
      }match {
        case scala.util.Success(resp) => resp
        case scala.util.Failure(f) => logger.error(f.getMessage,f)
                                      Future.successful{ Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
      }

    EitherT(out)
  }

  private def handleTries2[S,T](success:SuccessWrapper, fx: => Future[Either[Error,S]])( ff: S=>Right[ErrorWrapper,T] ): EitherT[Future,ErrorWrapper,T] = {

    val out:Future[Either[ErrorWrapper,T]] =
        fx.map{
          case Left(l) => val err = ErrorWrapper(l, success.steps)
            logger.warn(err.toString)
            scala.util.Success(Left(err))

          case Right(r) => scala.util.Success( ff(r) )

        }.recover{case e:Throwable=> scala.util.Failure(e)} map {
          case scala.util.Success(resp) => resp
          case scala.util.Failure(f) => logger.error(s"Future Failure:${f.getMessage}",f)
            Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )
        }

    EitherT(out)
  }

  /*
  def stepOver(tryresp: => Try[Future[Either[Error,Success]]] ): EitherT[Future,ErrorWrapper,SuccessWrapper]  = stepOver(SuccessWrapper(Success(None,None),0),tryresp)

  def stepOver(success:SuccessWrapper, tryresp: => Try[Future[Either[Error,Success]]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = {

    val out:Future[Either[ErrorWrapper,SuccessWrapper]] = tryresp match{
      case scala.util.Success(resp) => resp.map {
        case Left(l) => Left(ErrorWrapper(l, success.steps))
        case Right(r) => Right(SuccessWrapper(r, success.steps))
      }
      case scala.util.Failure(f) => println("ORROREEE"+f);Future{Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
    }

    EitherT(out)
  }

  def stepOver(resp:Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper]  = stepOver(SuccessWrapper(Success(None,None),0),resp)

  def stepOver(success:SuccessWrapper, resp:Future[Either[Error,Success]] ): EitherT[Future,ErrorWrapper,SuccessWrapper] = {

    val out:Future[Either[ErrorWrapper,SuccessWrapper]] = resp.map{
      case Left(l) => Left( ErrorWrapper(l,success.steps) )
      case Right(r) =>  Right( SuccessWrapper(r,success.steps) )
    }

    EitherT(out)
  }*/


/*
  def stepOverF[S](tryresp:Try[Future[Either[Error,S]]] ): EitherT[Future,ErrorWrapper,S] = stepOverF(SuccessWrapper(Success(None,None),0), tryresp )

  def stepOverF[S](success:SuccessWrapper, tryresp:Try[Future[Either[Error,S]]] ): EitherT[Future,ErrorWrapper,S]= {

    val out:Future[Either[ErrorWrapper,S]] = tryresp match{
      case scala.util.Success(resp) => resp.map {
        case Left(l) => Left(ErrorWrapper(l, success.steps))
        case Right(r) => Right(r)
      }
      case scala.util.Failure(f) => Future{Left( ErrorWrapper(Error(Option(0),Some(f.getMessage),None),success.steps) )}
    }

    EitherT(out)
  }


  def stepOverF[S](resp:Future[Either[Error,S]] ): EitherT[Future,ErrorWrapper,S] = stepOverF(SuccessWrapper(Success(None,None),0), resp )

  def stepOverF[S](success:SuccessWrapper, resp:Future[Either[Error,S]] ): EitherT[Future,ErrorWrapper,S]= {

    val out:Future[Either[ErrorWrapper,S]] = resp.map{
      case Left(l) => Left( ErrorWrapper(l,success.steps) )
      case Right(r) => Right(r)
    }

    EitherT(out)
  }
*/

  def unwrap(in:Future[Either[ErrorWrapper,SuccessWrapper]]):Future[Either[Error,Success]] = {
    in.map{
      case Left(l) => Left(l.error)
      case Right(r) => Right(r.success)
    }

  }

/*
  def stepOver2[S](success:SuccessWrapper, resp:Future[Either[Error,S]] )= {

    val out:Future[Either[ErrorWrapper,Any]] = resp.map{
      case Left(l) => Left( ErrorWrapper(l,success.steps) )
      case Right(r) =>  r match {
        case s:Success => Right( SuccessWrapper(s,success.steps) )
        case _ => Right(r)
      }
    }

    EitherT(out)
  }*/


}
