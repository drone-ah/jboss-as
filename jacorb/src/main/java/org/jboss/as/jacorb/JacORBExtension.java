/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jacorb;

import static org.jboss.as.jacorb.JacORBSubsystemConstants.CLIENT;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.IDENTITY;
import static org.jboss.as.jacorb.JacORBSubsystemConstants.SECURITY;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * <p>
 * The JacORB extension implementation.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JacORBExtension implements Extension {

    private static final JacORBSubsystemParser PARSER = JacORBSubsystemParser.INSTANCE;

    public static final String SUBSYSTEM_NAME = "jacorb";

    private static final String RESOURCE_NAME = JacORBExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, JacORBExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(JacORBSubsystemResource.INSTANCE);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(PARSER);

        if (context.isRegisterTransformers()) {
            // Register the model transformers
            registerTransformers(subsystem);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_0.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_1.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_2.getUriString(), PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JacORBSubsystemParser.Namespace.JacORB_1_3.getUriString(), PARSER);
    }

    /**
     * Register the transformers for older model versions.
     *
     * @param subsystem the subsystems registration
     */
    protected static void registerTransformers(final SubsystemRegistration subsystem) {
        final ModelVersion version110 = ModelVersion.create(1, 1, 0);
        final Set<String> expressionKeys = new HashSet<String>();
        for(final AttributeDefinition def : JacORBSubsystemDefinitions.ATTRIBUTES_BY_NAME.values()) {
            if(def.isAllowExpression()) {
                expressionKeys.add(def.getName());
            }
        }
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, expressionKeys.toArray(new String[expressionKeys.size()]))
            .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {

                @Override
                protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    final String security = attributeValue.asString();
                    //TODO this is adapted from the old SecurityInterceptorTransformer but it does not look totally right
                    if (security.equals(CLIENT) || security.equals(IDENTITY)) {
                        attributeValue.set("on");
                    }
                }
            }, SECURITY)
            .end();
        TransformationDescription.Tools.register(builder.build(), subsystem, version110);
    }

}
