package fr.solunea.thaleia.webapp.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.settings.IApplicationSettings;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * On redéfinit la classe FeedbackPanel à notre sauce pour pouvoir injecter des classes dans le DOM.
 */
public class ThaleiaFeedbackPanel extends Panel implements IFeedback {
    /**
     * List for messages.
     */
    private final class MessageListView extends ListView<FeedbackMessage>{
        private static final long serialVersionUID = 1L;

        /**
         * @see org.apache.wicket.Component#Component(String)
         */
        public MessageListView(final String id){
            super(id);
            setDefaultModel(newFeedbackMessagesModel());
        }

        @Override
        protected IModel<FeedbackMessage> getListItemModel(
                final IModel<? extends List<FeedbackMessage>> listViewModel, final int index)
        {
            return new AbstractReadOnlyModel<>() {
                private static final long serialVersionUID = 1L;

                /**
                 * WICKET-4258 Feedback messages might be cleared already.
                 *
                 * @see IApplicationSettings#setFeedbackMessageCleanupFilter(org.apache.wicket.feedback.IFeedbackMessageFilter)
                 */
                @Override
                public FeedbackMessage getObject() {
                    if (index >= listViewModel.getObject().size()) {
                        return null;
                    } else {
                        return listViewModel.getObject().get(index);
                    }
                }
            };
        }

        @Override
        protected void populateItem(final ListItem<FeedbackMessage> listItem) {
            final FeedbackMessage message = listItem.getModelObject();

            // CSS du FeedbackMessage
            final WebMarkupContainer messageComponent = new WebMarkupContainer("message");
            final AttributeModifier levelModifier = AttributeModifier.append("class", getCSSClass(message));
            messageComponent.add(levelModifier);
            listItem.add(messageComponent);

            // Texte du FeedbackMessage
            Serializable serializable = message.getMessage();
            Label messageLabel = new Label("label", (serializable == null) ? "" : serializable.toString());
            messageLabel.setEscapeModelStrings(ThaleiaFeedbackPanel.this.getEscapeModelStrings());
            messageComponent.add(messageLabel);

            // Attention à ne pas supprimer "message.markRendered()" ! FeedbackPanel efface automatiquement les
            // messages marqués rendered. Si les messages ne le sont plus, ils ne seront plus effacés et
            // réapparaîtrons entre les pages... Ca fait un peu désordre.
            message.markRendered();
        }

    }

    private static final long serialVersionUID = 1L;

    /** Message view */
    private final ThaleiaFeedbackPanel.MessageListView messageListView;

    /**
     * @see org.apache.wicket.Component#Component(String)
     */
    public ThaleiaFeedbackPanel(final String id){
        this(id, null);
    }

    /**
     * @see org.apache.wicket.Component#Component(String)
     *
     * @param id composant wicket.
     * @param filter filtre de messages.
     */
    public ThaleiaFeedbackPanel(final String id, IFeedbackMessageFilter filter){
        super(id);
        WebMarkupContainer messagesContainer = new WebMarkupContainer("feedbackul")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(anyMessage());
            }
        };
        add(messagesContainer);
        messageListView = new ThaleiaFeedbackPanel.MessageListView("messages");
        messageListView.setVersioned(false);
        messagesContainer.add(messageListView);

        if (filter != null) {
            setFilter(filter);
        }

        // On choisit d'interpréter le HTML dans les valeurs de messages, par exemple pour y inclure des liens.
        setEscapeModelStrings(false);

        this.setOutputMarkupId(true);
    }

    /**
     * Search messages that this panel will render, and see if there is any message of level ERROR
     * or up. This is a convenience method; same as calling 'anyMessage(FeedbackMessage.ERROR)'.
     *
     * @return whether there is any message for this panel of level ERROR or up
     */
    public final boolean anyErrorMessage(){
        return anyMessage(FeedbackMessage.ERROR);
    }

    /**
     * Search messages that this panel will render, and see if there is any message.
     *
     * @return whether there is any message for this panel
     */
    public final boolean anyMessage(){
        return anyMessage(FeedbackMessage.UNDEFINED);
    }

    /**
     * Search messages that this panel will render, and see if there is any message of the given
     * level.
     *
     * @param level
     *            the level, see FeedbackMessage
     * @return whether there is any message for this panel of the given level
     */
    public final boolean anyMessage(int level){
        List<FeedbackMessage> msgs = getCurrentMessages();

        for (FeedbackMessage msg : msgs)
        {
            if (msg.isLevel(level))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @return Model for feedback messages on which you can install filters and other properties
     */
    public final FeedbackMessagesModel getFeedbackMessagesModel(){
        return (FeedbackMessagesModel)messageListView.getDefaultModel();
    }

    /**
     * @return The current message filter
     */
    public final IFeedbackMessageFilter getFilter(){
        return getFeedbackMessagesModel().getFilter();
    }

    /**
     * @return The current sorting comparator
     */
    public final Comparator<FeedbackMessage> getSortingComparator(){
        return getFeedbackMessagesModel().getSortingComparator();
    }

    /**
     * @see org.apache.wicket.Component#isVersioned()
     */
    @Override
    public boolean isVersioned(){
        return false;
    }

    /**
     * Sets a filter to use on the feedback messages model
     *
     * @param filter
     *            The message filter to install on the feedback messages model
     *
     * @return FeedbackPanel this.
     */
    public final ThaleiaFeedbackPanel setFilter(IFeedbackMessageFilter filter){
        getFeedbackMessagesModel().setFilter(filter);
        return this;
    }

    /**
     * @param maxMessages
     *            The maximum number of feedback messages that this feedback panel should show at
     *            one time
     *
     * @return FeedbackPanel this.
     */
    public final ThaleiaFeedbackPanel setMaxMessages(int maxMessages) {
        messageListView.setViewSize(maxMessages);
        return this;
    }

    /**
     * Sets the comparator used for sorting the messages.
     *
     * @param sortingComparator
     *            comparator used for sorting the messages.
     *
     * @return FeedbackPanel this.
     */
    public final ThaleiaFeedbackPanel setSortingComparator(Comparator<FeedbackMessage> sortingComparator){
        getFeedbackMessagesModel().setSortingComparator(sortingComparator);
        return this;
    }

    /**
     * Gets the css class for the given message.
     *
     * @param message
     *            the message
     * @return the css class; by default, this returns feedbackPanel + the message level, eg
     *         'feedbackPanelERROR', but you can override this method to provide your own
     */
    protected String getCSSClass(final FeedbackMessage message) {
        String css;
        switch (message.getLevel()) {
            case FeedbackMessage.SUCCESS:
                css = "success";
                break;
            case FeedbackMessage.INFO:
                css = "info";
                break;
            case FeedbackMessage.WARNING:
                css = "warning";
                break;
            case FeedbackMessage.ERROR:
                css = "error";
                break;
            default:
                css = "";
        }

        return css;
    }

    /**
     * Gets the currently collected messages for this panel.
     *
     * @return the currently collected messages for this panel, possibly empty
     */
    protected final List<FeedbackMessage> getCurrentMessages()
    {
        final List<FeedbackMessage> messages = messageListView.getModelObject();
        return Collections.unmodifiableList(messages);
    }

    /**
     * Gets a new instance of FeedbackMessagesModel to use.
     *
     * @return Instance of FeedbackMessagesModel to use
     */
    protected FeedbackMessagesModel newFeedbackMessagesModel()
    {
        return new FeedbackMessagesModel(this);
    }

}
