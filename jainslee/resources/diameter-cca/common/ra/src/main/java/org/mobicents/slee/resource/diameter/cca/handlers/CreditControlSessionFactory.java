package org.mobicents.slee.resource.diameter.cca.handlers;

import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ClientCCASession;
import org.jdiameter.api.cca.ClientCCASessionListener;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.ServerCCASessionListener;
import org.jdiameter.api.cca.events.JCreditControlAnswer;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.impl.app.cca.ClientCCASessionImpl;
import org.jdiameter.common.api.app.IAppSessionFactory;
import org.jdiameter.common.api.app.cca.ICCAMessageFactory;
import org.jdiameter.common.api.app.cca.IClientCCASessionContext;
import org.jdiameter.common.api.app.cca.IServerCCASessionContext;
import org.jdiameter.common.impl.app.auth.ReAuthAnswerImpl;
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl;
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;

/**
 * 
 * CreditControlSessionFactory.java
 * 
 * <br>
 * Super project: mobicents <br>
 * 3:19:55 AM Dec 30, 2008 <br>
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class CreditControlSessionFactory implements IAppSessionFactory, ClientCCASessionListener, ServerCCASessionListener, StateChangeListener, ICCAMessageFactory, IServerCCASessionContext, IClientCCASessionContext {

  protected SessionFactory sessionFactory = null;
  protected CCASessionCreationListener resourceAdaptor = null;

  // Message timeout value (in milliseconds)

  protected int defaultDirectDebitingFailureHandling = 0;
  protected int defaultCreditControlFailureHandling = 0;

  // its seconds
  protected long defaultValidityTime = 30;
  protected long defaultTxTimerValue = 10;
  protected Logger logger = Logger.getLogger(CreditControlSessionFactory.class);

  public CreditControlSessionFactory(SessionFactory sessionFactory, CCASessionCreationListener resourceAdaptor) {
    super();

    this.sessionFactory = sessionFactory;
    this.resourceAdaptor = resourceAdaptor;
  }

  public CreditControlSessionFactory(SessionFactory sessionFactory, CCASessionCreationListener resourceAdaptor, int defaultDirectDebitingFailureHandling, int defaultCreditControlFailureHandling, long defaultValidityTime, long defaultTxTimerValue) {
    this(sessionFactory, resourceAdaptor);
    
    this.defaultDirectDebitingFailureHandling = defaultDirectDebitingFailureHandling;
    this.defaultCreditControlFailureHandling = defaultCreditControlFailureHandling;
    this.defaultValidityTime = defaultValidityTime;
    this.defaultTxTimerValue = defaultTxTimerValue;
  }

  public AppSession getNewSession(String sessionId, Class<? extends AppSession> aClass, ApplicationId applicationId, Object[] args) {
    AppSession appSession = null;
    try {
      if (aClass == ClientCCASession.class) {
        ClientCCASessionImpl clientSession = null;
        if (args != null && args.length > 0 && args[0] instanceof Request) {
          Request request = (Request) args[0];
          clientSession = new ClientCCASessionImpl(request.getSessionId(), this, sessionFactory, this);
        }
        else {
          clientSession = new ClientCCASessionImpl(sessionId, this, sessionFactory, this);
        }

        clientSession.getSessions().get(0).setRequestListener(clientSession);
        clientSession.addStateChangeNotification(this);

        this.resourceAdaptor.sessionCreated(clientSession);

        appSession = clientSession;
      }
      else if (aClass == ServerCCASession.class) {
        ServerCCASessionImpl serverSession = null;

        if (args != null && args.length > 0 && args[0] instanceof Request) {
          // This shouldnt happen but just in case
          Request request = (Request) args[0];
          serverSession = new ServerCCASessionImpl(request.getSessionId(), this, sessionFactory, this);
        }
        else {
          serverSession = new ServerCCASessionImpl(sessionId, this, sessionFactory, this);
        }

        serverSession.addStateChangeNotification(this);
        serverSession.getSessions().get(0).setRequestListener(serverSession);

        this.resourceAdaptor.sessionCreated(serverSession);

        appSession = serverSession;
      }
      else {
        throw new IllegalArgumentException("Wrong session class!![" + aClass + "]. Supported[" + ClientCCASession.class + "," + ServerCCASession.class + "]");
      }
    }
    catch (Exception e) {
      logger.error("Failure to obtain new Credit-Control Session.", e);
    }

    return appSession;
  }

  // ////////////////////
  // Message Handlers //
  // ////////////////////

  private void doMessage(AppSession appSession, AppEvent appEvent) throws InternalException {
    this.resourceAdaptor.fireEvent(appSession.getSessions().get(0).getSessionId(), appEvent.getMessage());
  }

  public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest request) throws InternalException {
    doMessage(session, request);
  }

  public void doCreditControlAnswer(ClientCCASession session, JCreditControlRequest request, JCreditControlAnswer answer) throws InternalException {
    doMessage(session, answer);
  }

  public void doReAuthRequest(ClientCCASession session, ReAuthRequest request) throws InternalException {
    doMessage(session, request);
  }

  public void doReAuthAnswer(ServerCCASession session, ReAuthRequest request, ReAuthAnswer answer) throws InternalException {
    doMessage(session, answer);
  }

  public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer) throws InternalException {
    // Here we get something weird, lets do extension
    // Still we rely on CCA termination mechanisms, those message are sent via generic send, which does not trigger FSM

    logger.info("Diameter CCA RA :: doOtherEvent :: appSession[" + session + "], Request[" + request + "], Answer[" + answer + "]");

    if (answer != null) {
      doMessage(session, answer);
    }
    else {
      doMessage(session, request);
    }
  }

  // ///////////////////////////
  // Message Factory Methods //
  // ///////////////////////////

  public JCreditControlAnswer createCreditControlAnswer(Answer answer) {
    return new JCreditControlAnswerImpl(answer);
  }

  public JCreditControlRequest createCreditControlRequest(Request req) {
    return new JCreditControlRequestImpl(req);
  }

  public ReAuthAnswer createReAuthAnswer(Answer answer) {
    return new ReAuthAnswerImpl(answer);
  }

  public ReAuthRequest createReAuthRequest(Request req) {
    return new ReAuthRequestImpl(req);
  }

  // ///////////////////
  // Context Methods //
  // ///////////////////

  public void stateChanged(Enum oldState, Enum newState) {
    if (logger.isInfoEnabled()) {
      logger.info("Diameter CCA SessionFactory :: stateChanged :: oldState[" + oldState + "], newState[" + newState + "]");
    }
  }

  public void sessionSupervisionTimerExpired(ServerCCASession session) {
    //this.resourceAdaptor.sessionDestroyed(session.getSessions().get(0).getSessionId(), session);
    session.release();
  }

  public void sessionSupervisionTimerReStarted(ServerCCASession session, ScheduledFuture future) {
    // TODO Complete this method.
  }

  public void sessionSupervisionTimerStarted(ServerCCASession session, ScheduledFuture future) {
    // TODO Complete this method.
  }

  public void sessionSupervisionTimerStopped(ServerCCASession session, ScheduledFuture future) {
    // TODO Complete this method.
  }

  public void timeoutExpired(Request request) {
    // FIXME What should we do when there's a timeout?
  }

  public void denyAccessOnDeliverFailure(ClientCCASession clientCCASessionImpl, Message request) {
    // TODO Complete this method.
  }

  public void denyAccessOnFailureMessage(ClientCCASession clientCCASessionImpl) {
    // TODO Complete this method.
  }

  public void denyAccessOnTxExpire(ClientCCASession clientCCASessionImpl) {
    //this.resourceAdaptor.sessionDestroyed(clientCCASessionImpl.getSessions().get(0).getSessionId(), clientCCASessionImpl);
    clientCCASessionImpl.release();
  }

  public int getDefaultCCFHValue() {
    return defaultCreditControlFailureHandling;
  }

  public int getDefaultDDFHValue() {
    return defaultDirectDebitingFailureHandling;
  }

  public long getDefaultTxTimerValue() {
    return defaultTxTimerValue;
  }

  public void grantAccessOnDeliverFailure(ClientCCASession clientCCASessionImpl, Message request) {
    // TODO Auto-generated method stub
  }

  public void grantAccessOnFailureMessage(ClientCCASession clientCCASessionImpl) {
    // TODO Auto-generated method stub
  }

  public void grantAccessOnTxExpire(ClientCCASession clientCCASessionImpl) {
    // TODO Auto-generated method stub
  }

  public void indicateServiceError(ClientCCASession clientCCASessionImpl) {
    // TODO Auto-generated method stub
  }

  public void txTimerExpired(ClientCCASession session) {
    //this.resourceAdaptor.sessionDestroyed(session.getSessions().get(0).getSessionId(), session);
    session.release();
  }

  public long[] getApplicationIds() {
    // FIXME: What should we do here?
    return new long[] { 4 };
  }

  public long getDefaultValidityTime() {
    return this.defaultValidityTime;
  }

}