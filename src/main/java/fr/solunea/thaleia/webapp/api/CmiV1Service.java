package fr.solunea.thaleia.webapp.api;

import com.google.gson.JsonObject;
import fr.solunea.thaleia.model.CmiData;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.CmiDataDao;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.model.dao.PublicationDao;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.authorization.AuthorizationException;
import org.wicketstuff.rest.annotations.MethodMapping;
import org.wicketstuff.rest.annotations.ResourcePath;
import org.wicketstuff.rest.annotations.parameters.HeaderParam;
import org.wicketstuff.rest.annotations.parameters.RequestBody;
import org.wicketstuff.rest.utils.http.HttpMethod;

import java.util.List;

@ResourcePath("/api/v1/cmi")
@SuppressWarnings("unused")
public class CmiV1Service extends ApiV1Service {

    private final static Logger logger = Logger.getLogger(CmiV1Service.class);


    @SuppressWarnings("unused")
    public CmiV1Service(ICayenneContextService contextService, Configuration configuration) {
        super(contextService, configuration);
    }

    @MethodMapping(value = "/suspend_data/{publicationId}/{email}", httpMethod = HttpMethod.PUT)
    @SuppressWarnings("unused")
    public JsonObject setSuspendData(@HeaderParam("Authorization") String token, String publicationId, String email,
                                     @RequestBody String data) {
        try {
            getToken(token, true);

            String errorMessage = "Cannot save the suspend_data.";
            String okMessage = "Suspend_data is saved.";

            JsonObject error = checkParameters(publicationId, email, data, errorMessage);
            if (error != null) {
                return error;
            }

            // On prépare un contexte, pour isoler les modifications et les commiter en cas de réussite.
            ObjectContext context = ThaleiaApplication.get().contextService.getNewContext();

            // On vérifie que la publication existe
            PublicationDao publicationDao = new PublicationDao(context);
            Publication publication = publicationDao.findByReference(publicationId);
            if (publication == null) {
                return error(404, errorMessage, "Publication '" + publicationId + "' does not exist.");
            }

            try {
                // L'objet à stocker
                CmiData suspendData = getCmiData(email, publication, context);
                suspendData.setSuspendData(data);
                CmiDataDao cmiDataDao = new CmiDataDao(context);
                cmiDataDao.save(suspendData, false);
                context.commitChanges();

                return ok(0, okMessage);
            } catch (DetailedException e) {
                return error(500, errorMessage, e.getMessage());
            }
        } catch (AuthorizationException e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return new JsonObject();
        }
    }

    private JsonObject checkParameters(String publicationId, String email, Object data, String errorMessage) {
        if (publicationId == null || publicationId.isEmpty()) {
            return error(400, errorMessage, "The publicationId parameter is null.");
        }
        if (email == null || email.isEmpty()) {
            return error(400, errorMessage, "The email parameter is null.");
        }
        if (data == null) {
            return error(400, errorMessage, "The data in body request is null.");
        }
        return null;
    }

    /**
     * La modification sera faite dans le contexte, mais pas stockée en base.
     */
    private CmiData getCmiData(String email, Publication publication, ObjectContext context) {
        // L'objet à stocker
        CmiData cmiData;

        // Obtention de l'objet, si existant
        List<CmiData> cmiDataList = cmiDataService.find(publication, email);

        if (cmiDataList.isEmpty()) {
            // Création de l'objet si pas d'existant
            CmiDataDao cmiDataDao = new CmiDataDao(context);
            cmiData = cmiDataDao.get();
            cmiData.setEmail(email);
            cmiData.setPublication(publication);
        } else {
            // On prend le dernier trouvé
            cmiData = cmiDataList.get(cmiDataList.size() - 1);
        }
        return cmiData;
    }

    @MethodMapping("/suspend_data/{publicationReference}/{email}")
    @SuppressWarnings("unused")
    public String getSuspendData(@HeaderParam("Authorization") String token, String publicationReference,
                                 String email) {
        try {
            getToken(token, true);

            List<CmiData> suspendData = cmiDataService.find(publicationReference, email);

            if (suspendData.isEmpty()) {
                setResponseStatusCode(204);
                return null;
            } else {
                return suspendData.get(0).getSuspendData();
            }
        } catch (AuthorizationException e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return "";
        }

    }

    @MethodMapping("/all/{publicationReference}/{email}")
    @SuppressWarnings("unused")
    public CmiDataPOJO getAllData(@HeaderParam("Authorization") String token, String publicationReference,
                                  String email) {
        // On renvoie un objet CmiDataPOJO, car un objet de type CmiData est transmis de cette façon :
        //        {
        //            "snapshotVersion": -9223372036854774626,
        //                "values": {
        //            "publication": {
        //
        //            },
        //            "suspendData": "plop!",
        //                    "email": "toto@solunea.fr"
        //        },
        //            "objectId": {
        //            "entityName": "CmiData",
        //                    "singleKey": "id",
        //                    "singleValue": 200
        //        },
        //            "persistenceState": 3
        //        }

        try {
            getToken(token, true);
            List<CmiData> suspendData = cmiDataService.find(publicationReference, email);

            if (suspendData.isEmpty()) {
                setResponseStatusCode(204);
                return null;
            } else {
                return new CmiDataPOJO(suspendData.get(0));
            }
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }

    }

    @MethodMapping(value = "/all/{publicationId}/{email}", httpMethod = HttpMethod.PUT)
    @SuppressWarnings("unused")
    public JsonObject setAllData(@HeaderParam("Authorization") String token, String publicationId, String email,
                                 @RequestBody CmiDataPOJO cmiDataPOJO,
                                 @HeaderParam("Content-Type") String contentType) {
        try {
            getToken(token, true);

            // On demande explicitement un entête Content-Type=application/json
            if (contentType.isEmpty() || !contentType.equals("application/json")) {
                setResponseStatusCode(415);
                return null;
            }

            String errorMessage = "Cannot save the data.";
            String okMessage = "Data is saved.";

            JsonObject error = checkParameters(publicationId, email, cmiDataPOJO, errorMessage);
            if (error != null) {
                return error;
            }

            // On prépare un contexte, pour isoler les modifications et les commiter en cas de réussite.
            ObjectContext context = ThaleiaApplication.get().contextService.getNewContext();

            // On vérifie que la publication existe
            PublicationDao publicationDao = new PublicationDao(context);
            Publication publication = publicationDao.findByReference(publicationId);
            if (publication == null) {
                return error(404, errorMessage, "Publication '" + publicationId + "' does not exist.");
            }

            try {
                // On copie les valeurs transmises en base (sauf email et publication_reference qui sont dans le JSON
                // : on ne prend que ceux passés en paramètre de l'URL).
                CmiData cmiData = getCmiData(email, publication, context);
                cmiDataPOJO.copyValuesToCmiData(cmiData);
                CmiDataDao cmiDataDao = new CmiDataDao(context);
                cmiDataDao.save(cmiData, false);
                context.commitChanges();

                return ok(0, okMessage);
            } catch (DetailedException e) {
                return error(500, errorMessage, e.getMessage());
            }
        } catch (AuthorizationException e) {
            logger.warn(e);
            setResponseStatusCode(403);
            return new JsonObject();
        }
    }

    static class CmiDataPOJO {
        // Les noms de ces variables apparaissent tels quels dans l'objet JSON de sérialisation / désérialisation.
        private String completion_status;
        private String email;
        private String entry;
        private String exit;
        private String location;
        private String publication_reference;
        private String score_raw;
        private String session_time;
        private String success_status;
        private String suspend_data;
        private String total_time;

        public CmiDataPOJO(String completionStatus, String email, String entry, String exit, String location,
                           String publicationReference, String scoreRaw, String sessionTime, String successStatus,
                           String suspendData, String totalTime) {
            this.completion_status = completionStatus;
            this.email = email;
            this.entry = entry;
            this.exit = exit;
            this.location = location;
            this.publication_reference = publicationReference;
            this.score_raw = scoreRaw;
            this.session_time = sessionTime;
            this.success_status = successStatus;
            this.suspend_data = suspendData;
            this.total_time = totalTime;
        }

        public CmiDataPOJO(CmiData cmiData) {
            if (cmiData == null) {
                logger.warn("Paramètre nul !");
                return;
            }
            if (cmiData.getPublication() == null) {
                logger.warn("Paramètre publication nul !");
                return;
            }

            this.completion_status = cmiData.getCompletionStatus();
            this.email = cmiData.getEmail();
            this.entry = cmiData.getEntry();
            this.exit = cmiData.getExit();
            this.location = cmiData.getLocation();
            this.publication_reference = cmiData.getPublication().getReference();
            this.score_raw = cmiData.getScoreRaw();
            this.session_time = cmiData.getSessionTime();
            this.success_status = cmiData.getSuccessStatus();
            this.suspend_data = cmiData.getSuspendData();
            this.total_time = cmiData.getTotalTime();
        }

        /**
         * Copie toutes les valeurs dans cet objet cmiData, sauf email et publicationReference
         */
        void copyValuesToCmiData(CmiData cmiData) {
            cmiData.setCompletionStatus(this.completion_status);
            cmiData.setEntry(this.entry);
            cmiData.setExit(this.exit);
            cmiData.setLocation(this.location);
            cmiData.setScoreRaw(this.score_raw);
            cmiData.setSessionTime(this.session_time);
            cmiData.setSuccessStatus(this.success_status);
            cmiData.setSuspendData(this.suspend_data);
            cmiData.setTotalTime(this.total_time);
        }
    }
}
