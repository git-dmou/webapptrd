package fr.solunea.thaleia.webapp.pages.admin;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.BuyProcessDao;
import fr.solunea.thaleia.model.dao.LicenceDao;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.UploadFormPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BuyProcessesPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(BuyProcessesPanel.class);

    // Le formatteur de dates
    FastDateFormat format;

    /**
     * Le panneau de tous les achats réalisés par cet utilisateur
     */
    public BuyProcessesPanel(String id, IModel<User> user) {
        super(id, user);

        try {
            format = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT, ThaleiaSession.get().getLocale());

            final LicenceService licenceService = ThaleiaSession.get().getLicenceService();
            final LicenceDao licenceDao = new LicenceDao(ThaleiaSession.get().getContextService().getContextSingleton());

            // Le panneau des messages
            ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
            feedbackPanel.setOutputMarkupId(true);
            add(feedbackPanel);

            // Le lien vers la page d'achat de licence, actif si l'application le permet
            String redirectUrl = ThaleiaApplication.get().getApplicationRootUrl() + ThaleiaApplication.get().getConfiguration().getLicencesPageMountPoint();
            add(new ExternalLink("getLicenceLink", redirectUrl) {
                @Override
                public boolean isVisible() {
                    return ThaleiaApplication.get().getConfiguration().isOnlineLicenceBuyActivated();
                }
            });

            // Le nom de la licence en cours pour cet utilisateur
            add(new Label("currentLicence", new Model<String>() {
                @Override
                public String getObject() {
                    try {
                        StringBuilder licences = new StringBuilder();
                        String separator = " / ";
                        for (LicenceHolding licenceHolding : ThaleiaApplication.get().getLicenceHoldingDao().getValidHoldings(user.getObject())) {
                            String licenceName = ThaleiaSession.get().getLicenceService().getLicenceLocalizedName(licenceHolding, ThaleiaSession.get().getLocale());
                            String endDateAsString = format.format(licenceHolding.getEndDate());
                            licences.append(new StringResourceModel("currentLicence", BuyProcessesPanel.this, null, new Object[]{licenceName, endDateAsString}).getString());
                            licences.append(separator);
                        }
                        String result = licences.toString();
                        if (result.endsWith(separator)) {
                            result = result.substring(0, result.lastIndexOf(separator));
                        }
                        return result;
                    } catch (Exception e) {
                        logger.warn(e);
                        return " - ";
                    }
                }
            }));

            // Préparation des lignes du tableau
            add(new PropertyListView<>("datatable", new AbstractReadOnlyModel<List<BuyProcess>>() {
                @Override
                public List<BuyProcess> getObject() {
                    // On ne présente que les buyProcess payés par l'utilisateur
                    BuyProcessDao buyProcessDao = new BuyProcessDao(ThaleiaSession.get().getContextService().getContextSingleton());
                    List<BuyProcess> result = buyProcessDao.listByPayer(user.getObject(),
                            true);
                    // On trie du plus récent au plus vieux
                    Comparator<BuyProcess> comparator = buyProcessDao.getExecutionDateComparator();
                    result.sort(comparator);
                    Collections.reverse(result);
                    return result;
                }
            }) {

                @Override
                public boolean isVisible() {
                    return !getModelObject().isEmpty();
                }

                @Override
                protected void populateItem(final ListItem<BuyProcess> item) {

                    // On récupère la licence qui correspond au SKU demandé
                    Licence licence = licenceDao.findBySku(item.getModelObject().getSkuGiven());

                    item.add(new Label("executionDate", new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject() {
                            // On formatte l'horodate, si elle existe
                            if (item.getModelObject().getExecutionDate() == null) {
                                return "";
                            } else {
                                return format.format(item.getModelObject().getExecutionDate());
                            }
                        }
                    }));

                    item.add(new Label("licence", new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject() {
                            if (licence == null) {
                                logger.debug("Pas de licence pour le process : " + item.getModelObject());
                                return "";
                            } else {
                                // On présente le nom de la licence
                                return licenceService.getDisplayName(licence, ThaleiaSession.get().getLocale());
                            }
                        }
                    }));

                    item.add(new Label("validity", new AbstractReadOnlyModel<String>() {
                        @Override
                        public String getObject() {
                            // Attention : on n'est pas capable de récupérer l'objet LicenceHolding, qui contient la
                            // date de fin réelle de cette attribution de licence. Donc ce qu'on fait, c'est
                            // reconstruire cette date d'après la date d'achat et la durée de la licence.
                            if (licence == null) {
                                logger.debug("Pas de licence pour le process : " + item.getModelObject());
                                return "";
                            } else {
                                long durationMillis = (long) licence.getLicenceDurationDays() * (long) 3600000 * 24;
                                Date endDate = new Date(item.getModelObject().getExecutionDate().getTime() + durationMillis);
                                return format.format(endDate);
                            }
                        }
                    }));

                    // Un message qui indique que la facture est en préparation
                    WebMarkupContainer editingInvoice = (WebMarkupContainer) new WebMarkupContainer("editingInvoice") {
                        @Override
                        public boolean isVisible() {
                            // On montre le message si le nom de la facture est null ou vide
                            return ((item.getModelObject().getInvoice() == null)
                                    || (item.getModelObject().getInvoice().isEmpty()));
                        }
                    }.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
                    item.add(editingInvoice);
                    editingInvoice.add(new Image("waiting", new PackageResourceReference(getClass(), "ico_encours" + ".png")) {
                        @Override
                        public boolean isVisible() {
                            return editingInvoice.isVisible();
                        }
                    });

                    // Un lien de téléchargement de la facture, si elle existe
                    DownloadLink downloadInvoiceLink = new DownloadLink("downloadInvoice", new
                            AbstractReadOnlyModel<>() {
                                @Override
                                public File getObject() {
                                    try {
                                        // Récupération du binaire de la facture
                                        return ThaleiaSession.get().getBuyProcessService().getInvoice(item.getModelObject());
                                    } catch (DetailedException e) {
                                        logger.warn(e.toString());
                                        error(new StringResourceModel("invoice.download.error", BuyProcessesPanel.this, null, (Object[]) null).getString());
                                        return null;
                                    }
                                }
                            }
                    ) {
                        @Override
                        public boolean isVisible() {
                            // On ne montre pas le lien si le nom de la facture est null ou vide
                            return !((item.getModelObject().getInvoice() == null)
                                    || (item.getModelObject().getInvoice().isEmpty()));
                        }
                    }.setCacheDuration(Duration.NONE);
                    item.add(downloadInvoiceLink);
                    downloadInvoiceLink.add(new Image("facture", new PackageResourceReference(getClass(), "ico_fichier.png")) {
                        @Override
                        public boolean isVisible() {
                            return downloadInvoiceLink.isVisible();
                        }
                    });

                    // On associe un fichier temporaire pour récolter le binaire uploadé
                    UploadFormPanel uploadPanel;
                    try {
                        uploadPanel = new UploadFormPanel("uploadForm", Model.of(ThaleiaApplication.get().getTempFilesService().getTempFile()), true) {
                            @Override
                            public boolean isVisible() {
                                // Uniquement visible aux admins
                                return ThaleiaSession.get().getAuthenticatedUser().getIsAdmin();
                            }

                            @Override
                            public void onUpload(File uploadedFile, String filename, Locale locale, AjaxRequestTarget
                                    target) {
                                // On enregistre le fichier uploadé comme la nouvelle facture
                                try {
                                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                                    BuyProcess modelObject = context.localObject(item.getModelObject());

                                    ThaleiaSession.get().getBuyProcessService().setInvoice(modelObject, uploadedFile, filename);
                                    context.commitChanges();

                                    info(new StringResourceModel("invoice.updated", BuyProcessesPanel.this, null, new Object[]{filename}).getString());

                                    // On rafraîchit l'item pour présenter la modification du modèle.
                                    target.add(item);
                                    target.add(feedbackPanel);

                                    // Le message qui indique que la facture est en préparation
                                    target.add(editingInvoice);

                                } catch (DetailedException e) {
                                    item.getModel().getObject().getObjectContext().rollbackChanges();
                                    logger.warn("Erreur d'enregistrement du fichier uploadé :" + e);
                                    StringResourceModel errorMessageModel = new StringResourceModel("upload.error",
                                            this, null);
                                    Session.get().error(errorMessageModel.getString());
                                }
                            }
                        };
                        item.add(uploadPanel.setOutputMarkupId(true));
                    } catch (DetailedException e) {
                        logger.warn("Impossible d'obtenir un fichier temporaire pour traiter un upload de facture !");
                        logger.warn(e);
                        item.add(new EmptyPanel("uploadForm"));
                    }
                    item.setOutputMarkupId(true);

                }
            });

            add(new Link<Void>("cgvLink") {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("target", "_blank");
                }

                @Override
                public void onClick() {
                    // On ouvre le PDF des CGV dans la nouvelle page
                    if (ThaleiaSession.get().getLocale().equals(java.util.Locale.FRENCH)) {
                        throw new RedirectToUrlException(ThaleiaApplication.get().getConfiguration().getCgvMountPoint());
                    } else {
                        throw new RedirectToUrlException(ThaleiaApplication.get().getConfiguration().getCgvMountPointEN());
                    }
                }
            });
            add(new Link<Void>("usageLink") {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("target", "_blank");
                }

                @Override
                public void onClick() {
                    // On ouvre la page des Conditions d'Utilisation dans la nouvelle page
                    throw new RedirectToUrlException("/" + Configuration.LEGAL_SITE);
                }
            });

        } catch (Exception e) {
            logger.error("Erreur : " + LogUtils.getStackTrace(e.getStackTrace()));
            setResponsePage(new ErrorPage());
        }

    }
}
