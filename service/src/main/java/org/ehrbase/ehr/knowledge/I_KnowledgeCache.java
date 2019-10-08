/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.ehr.knowledge;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import openEHR.v1.template.TEMPLATE;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public interface I_KnowledgeCache {

    String TEMPLATE_ID = "templateId";

    /**
     * Adds operational template to system and also in current cache.
     * @param content operational template input
     * @return resulting template ID, when successful
     * @throws InvalidApiParameterException when input can't be pared to OPT instance
     * @throws StateConflictException when template with same template ID is already in the system
     * @throws InternalServerException when an unspecified problem occurs
     */
    String addOperationalTemplate(byte[] content);

    List<TemplateMetaData> listAllOperationalTemplates() throws IOException;


    /**
     * return a map of identifier and File for a defined search pattern.
     * Includes and excludes are regular expression used to refine searches
     *
     * @param includes a regexp for files to include in search, null if all files must be added
     * @param excludes a regexp for files to exclude from search, null if all files must be opted out
     * @return a Map of files corresponding to the specified filters
     */
    Map<String, File> retrieveFileMap(Pattern includes, Pattern excludes);



    /**
     * retrieve a template associated to a key
     *
     * @param key a template name
     * @return a TEMPLATE document instance or null
     * @throws Exception
     * @see openEHR.v1.template.TEMPLATE
     */
    TEMPLATE retrieveOpenehrTemplate(String key);

    /**
     * retrieve an operational template document instance
     *
     * @param key the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key);

    /**
     * retrieve a <b>cached</b> operational template document instance using its unique Id
     *
     * @param uuid the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @throws Exception
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid);

    /**
     * retrieve a <b>cached</b> template document instance using its unique Id
     *
     * @param uuid the name of the operational template
     * @return a TEMPLATE document instance or null
     * @throws Exception
     * @see openEHR.v1.template.TEMPLATE
     */
    TEMPLATE retrieveTemplate(UUID uuid);


    String settings();


}
