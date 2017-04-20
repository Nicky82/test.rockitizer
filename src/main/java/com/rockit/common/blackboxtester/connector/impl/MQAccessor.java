package com.rockit.common.blackboxtester.connector.impl;import static com.rockit.common.blackboxtester.suite.configuration.SettingsHolder.cacheByConnector;import java.io.IOException;import java.util.HashMap;import java.util.Map;import org.apache.log4j.Logger;import com.ibm.mq.MQEnvironment;import com.ibm.mq.MQException;import com.ibm.mq.MQGetMessageOptions;import com.ibm.mq.MQMessage;import com.ibm.mq.MQPutMessageOptions;import com.ibm.mq.MQQueue;import com.ibm.mq.MQQueueManager;import com.ibm.mq.constants.CMQC;import com.rockit.common.blackboxtester.connector.settings.MQHeader;import com.rockit.common.blackboxtester.exceptions.ConnectorException;public abstract class MQAccessor {	public static final Logger LOGGER = Logger.getLogger(MQAccessor.class.getName());	static Map<String, MQQueueManager> cache = new HashMap<>();	protected String qManager;	protected String qName;	protected String message;	protected String messageId;	protected String channelname;	protected int port;	protected String hostname;	private final QueueOption qOption;	private String applicationIdData;	public MQAccessor(final String qManager, final String qName, final int port, final String channelname,			final String hostname) {		this.qManager = qManager;		this.hostname = hostname;		this.port = port;		this.channelname = channelname;		this.qName = qName;		this.qOption = QueueOption.GenerateMsgId;	}	public String get() {		String data = null;		try {			final MQQueueManager qMgr = newMQQueueManager();			final int openOptions = CMQC.MQOO_INQUIRE + CMQC.MQOO_FAIL_IF_QUIESCING + CMQC.MQOO_INPUT_SHARED;			final MQQueue queue = qMgr.accessQueue(qName, openOptions);			final MQMessage msg = new MQMessage();			final MQGetMessageOptions gmo = new MQGetMessageOptions();			if (queue.getCurrentDepth() > 0) {				queue.get(msg, gmo);				data = msg.readStringOfByteLength(msg.getDataLength());			}			if (queue.isOpen()) {				queue.close();			}		} catch (MQException | IOException e) {			LOGGER.error("Can not read queue: " + qName, e);			throw new ConnectorException(e);		}		return data;	}	private void write(final MQQueue queue, final String name, final String message) {		final MQMessage mQMsg = new MQMessage();		final MQHeader mQHeader = (MQHeader) cacheByConnector(getType(), getId());		// LOGGER.info( "MQHeader " + mQHeader.toString() );		mQMsg.characterSet = mQHeader.getCodedCharSetId();		mQMsg.format = mQHeader.getMsgFormat();		mQMsg.messageType = mQHeader.getMsgType();		mQMsg.correlationId = mQHeader.getCorrelId();		mQMsg.expiry = mQHeader.getExpiry();		mQMsg.messageId = mQHeader.getMsgId();		mQMsg.replyToQueueName = mQHeader.getReplyToQ();		mQMsg.replyToQueueManagerName = mQHeader.getReplyToQMgr();		if (name != null) {			mQMsg.messageId = name.getBytes();		}		if (applicationIdData != null && !applicationIdData.isEmpty()) {			mQMsg.applicationIdData = applicationIdData;		}		try {			mQMsg.write(message.getBytes());			final MQPutMessageOptions pmo = new MQPutMessageOptions();			switch (qOption) {						case SetMsgId:				pmo.options |= CMQC.MQPMO_SET_ALL_CONTEXT;// Broker, MsgId wird gesetzt				break;							case SetMsgIdAndUserId:				pmo.options |= CMQC.MQPMO_SET_IDENTITY_CONTEXT;// Vitria, MsgId und UserId wird gesetzt									mQMsg.userId = "unknown";			default:			}			queue.put(mQMsg, pmo);			LOGGER.info("Message successfully written to " + queue.getName());		} catch (IOException | MQException ex) {			LOGGER.error("queue not be written: " + qName, ex);			throw new ConnectorException(ex);		}	}	public void putMessage(final String message, final String name) {		try {			final MQQueueManager qMgr = newMQQueueManager();			int openOptions = CMQC.MQOO_OUTPUT;// Standard, MsgId wird generiert			switch (this.qOption) {			case SetMsgId:				openOptions = CMQC.MQOO_OUTPUT | CMQC.MQOO_SET_ALL_CONTEXT;// Broker, MsgId wird gesetzt																	 				break;			case SetMsgIdAndUserId:				openOptions = CMQC.MQOO_OUTPUT | CMQC.MQOO_SET_IDENTITY_CONTEXT;// Vitria, MsgId und UserId wird gesetzt				break;			default:			}			final MQQueue queue = qMgr.accessQueue(qName, openOptions);			write(queue, name, message);// Writing message into the queue			queue.close();		} catch (final MQException ex) {			LOGGER.error("can not write queue: " + qName, ex);			throw new ConnectorException(ex);		}	}	private MQQueueManager newMQQueueManager() {		MQEnvironment.hostname = hostname; // host to connect to		MQEnvironment.port = port; // port to connect to.		MQEnvironment.channel = channelname; // the CASE-SENSITIVE		if (null == cache.get(qManager)) {			LOGGER.info("Connecting MQQueueManager " + qManager);			try {				cache.put(qManager, new MQQueueManager(qManager));			} catch (final MQException e) {				LOGGER.error("MQQueueManager not available: " + qManager, e);				throw new ConnectorException(e);			}		}		return cache.get(qManager);	}		private enum QueueOption {		GenerateMsgId("GenerateMsgId"), SetMsgId("SetMsgId"), SetMsgIdAndUserId("SetMsgIdAndUserId");		private String opt;		private QueueOption(final String opt) {			this.opt = opt;		}	}	public String getQName() {		return qName;	}	public String getMessageId() {		return messageId;	}	public void setMessageId(final String messageId) {		this.messageId = messageId;	}		@Override	public String toString() {		return "WebsphereMQ(qManager=" + this.qManager + ", qName=" + this.qName + ", hostname=" + this.hostname				+ ", port=" + this.port + ", channelname=" + this.channelname + ")";	}	public abstract String getId();	public abstract String getType();	/*	 * private void closeQueueMgr(MQQueueManager qMgr) throws MQException {	 * System.out.println("Disconnecting from the Queue Manager");	 * qMgr.disconnect(); }	 */	// private void setQueueOption(String opt) {	// QueueOption option = QueueOption.getOption(opt);	// if (option != null) {	// this.qOption = option;	// }	// }	//	// private void setApplicationIdData(String applicationIdData) {	// this.applicationIdData = applicationIdData;	// }}