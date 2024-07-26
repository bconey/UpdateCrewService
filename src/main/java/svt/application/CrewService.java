/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package svt.application;

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.Json;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/crew")
@ApplicationScoped
public class CrewService {

    @Inject MongoDatabase db;
    @Inject Validator validator;
    private JsonArray getViolations(CrewMember crewMember) {
        Set<ConstraintViolation<CrewMember>> violations = validator.validate(
                crewMember);

        JsonArrayBuilder messages = Json.createArrayBuilder();

        for (ConstraintViolation<CrewMember> v : violations) {
            messages.add(v.getMessage());
        }

        return messages.build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully updated crew member."),
        @APIResponse(responseCode = "400", description = "Invalid object id or crew member configuration."),
        @APIResponse(responseCode = "404", description = "Crew member object id was not found.") })
    @Operation(summary = "Update a crew member in the database.")
    public Response update(CrewMember crewMember,
        @Parameter(description = "Object id of the crew member to update.", required = true)
        @PathParam("id") String id) {

        JsonArray violations = getViolations(crewMember);

        if (!violations.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(violations.toString()).build();
        }

        ObjectId oid;

        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("[\"Invalid object id!\"]").build();
        }

        MongoCollection<Document> crew = db.getCollection("Crew");

        Document query = new Document("_id", oid);

        Document newCrewMember = new Document();
        newCrewMember.put("Name", crewMember.getName());
        newCrewMember.put("Rank", crewMember.getRank());
        newCrewMember.put("CrewID", crewMember.getCrewID());

        UpdateResult updateResult = crew.replaceOne(query, newCrewMember);

        if (updateResult.getMatchedCount() == 0) {
            return Response.status(Response.Status.NOT_FOUND).entity("[\"_id was not found!\"]").build();
        }

        newCrewMember.put("_id", oid);

        return Response.status(Response.Status.OK).entity(newCrewMember.toJson()).build();
    }

}
