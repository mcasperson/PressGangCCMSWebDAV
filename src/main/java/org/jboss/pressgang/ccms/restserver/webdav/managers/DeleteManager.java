package org.jboss.pressgang.ccms.restserver.webdav.managers;

import org.jboss.pressgang.ccms.restserver.webdav.WebDavConstants;

import javax.enterprise.context.ApplicationScoped;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Database fields can not be deleted, but when they are exposed as files, some applications expect to be able to delete
 * them (e.g. Kate will delete the file it is editing, and then check to make sure it is deleted before saving any changes).
 * This manager simply keeps a track of delete requests, and shows a file as being deleted for a short period of time,
 * or until it is "created" again.
 */
@ApplicationScoped
public class DeleteManager {

    final Map<ResourceTypes, HashMap<Integer, Calendar>> deletedResources = new HashMap<ResourceTypes, HashMap<Integer, Calendar>>();

    synchronized public boolean isDeleted(final ResourceTypes resourceType, final Integer id) {
        if (deletedResources.containsKey(resourceType)) {
            final HashMap<Integer, Calendar> specificDeletedResources = deletedResources.get(resourceType);
            if (specificDeletedResources.containsKey(id)) {
                final Calendar deletionDate = specificDeletedResources.get(id);
                final Calendar window = Calendar.getInstance();
                window.add(Calendar.SECOND, -WebDavConstants.DELETE_WINDOW);

                if (deletionDate.before(window)) {
                    specificDeletedResources.remove(id);
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    synchronized public void delete(final ResourceTypes resourceType, final Integer id) {
        if (!deletedResources.containsKey(resourceType)) {
            deletedResources.put(resourceType, new HashMap<Integer, Calendar>());
        }

        deletedResources.get(resourceType).put(id, Calendar.getInstance());
    }

    synchronized public void create(final ResourceTypes resourceType, final Integer id) {
        if (!deletedResources.containsKey(resourceType)) {
            deletedResources.put(resourceType, new HashMap<Integer, Calendar>());
        }

        if (deletedResources.get(resourceType).containsKey(id)) {
            deletedResources.get(resourceType).remove(id);
        }
    }
}
