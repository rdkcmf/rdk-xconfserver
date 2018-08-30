/* 
 * If not stated otherwise in this file or this component's Licenses.txt file the 
 * following copyright and licenses apply:
 *
 * Copyright 2018 RDK Management
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
 *
 * Author: Igor Kostrov
 * Created: 2/4/2016
*/
package com.comcast.xconf.migration;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.dcm.converter.DcmRuleConverter;
import com.comcast.xconf.dcm.converter.FormulaConverter;
import com.comcast.xconf.dcm.manager.web.FormulaDataObject;
import com.comcast.xconf.logupload.DCMGenericRule;
import com.comcast.xconf.logupload.Formula;
import com.comcast.xconf.utils.annotation.Migration;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("migration")
@Migration
public class FormulaMigrationController {

    private static Logger log = LoggerFactory.getLogger(FormulaMigrationController.class);

    @Autowired
    private DcmRuleConverter converter;

    @Autowired
    private ISimpleCachedDAO<String, DCMGenericRule> dcmRuleDAO;

    /**
     * Migrate old formulas into DCM rules.
     */
    @RequestMapping(value = "/formulas", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    /*@Migration(oldKey = String.class, oldEntity = Formula.class, newKey = String.class, newEntity = DCMGenericRule.class, migrationURL = "/formulas")*/
    public ResponseEntity migrateFormulasIntoRules() {
        ISimpleCachedDAO<String, Formula> formulaDAO = DaoFactory.Simple.createCachedDAO(String.class, Formula.class);

        Iterable<Formula> formulas = Optional.presentInstances(formulaDAO.asLoadingCache().asMap().values());
        FormulaConverter formulaConverter = new FormulaConverter();
        int success = 0;
        int failed = 0;
        for (Formula formula : formulas) {
            try {
                log.info("Copying formula '{}' into DcmRule CF. Id: {}", formula.getName(), formula.getId());
                FormulaDataObject formulaDataObject = formulaConverter.convertToFormulaDataObject(formula);
                DCMGenericRule rule = converter.convertToRule(formulaDataObject);
                dcmRuleDAO.setOne(rule.getId(), rule);
                success++;
            } catch (ValidationException e) {
                log.error("Could not validate", e);
                failed++;
            } catch (Exception e) {
                log.error("Could not save formula as DCM rule", e);
                failed++;
            }
        }

        return ResponseEntity.ok("Successfully saved: " + success + ", failed: " + failed);

    }

}
