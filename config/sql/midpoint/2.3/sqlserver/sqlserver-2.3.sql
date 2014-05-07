CREATE TABLE m_abstract_role (
  approvalProcess NVARCHAR(255) COLLATE database_default,
  requestable     BIT,
  oid             NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_assignment (
  id                      SMALLINT                              NOT NULL,
  owner_oid               NVARCHAR(36) COLLATE database_default NOT NULL,
  administrativeStatus    INT,
  archiveTimestamp        DATETIME2,
  disableReason           NVARCHAR(255) COLLATE database_default,
  disableTimestamp        DATETIME2,
  effectiveStatus         INT,
  enableTimestamp         DATETIME2,
  validFrom               DATETIME2,
  validTo                 DATETIME2,
  validityChangeTimestamp DATETIME2,
  validityStatus          INT,
  assignmentOwner         INT,
  createChannel           NVARCHAR(255) COLLATE database_default,
  createTimestamp         DATETIME2,
  creatorRef_relation     NVARCHAR(157) COLLATE database_default,
  creatorRef_targetOid    NVARCHAR(36) COLLATE database_default,
  creatorRef_type         INT,
  modifierRef_relation    NVARCHAR(157) COLLATE database_default,
  modifierRef_targetOid   NVARCHAR(36) COLLATE database_default,
  modifierRef_type        INT,
  modifyChannel           NVARCHAR(255) COLLATE database_default,
  modifyTimestamp         DATETIME2,
  orderValue              INT,
  targetRef_relation      NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid     NVARCHAR(36) COLLATE database_default,
  targetRef_type          INT,
  tenantRef_relation      NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid     NVARCHAR(36) COLLATE database_default,
  tenantRef_type          INT,
  extId                   SMALLINT,
  extOid                  NVARCHAR(36) COLLATE database_default,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_assignment_ext_date (
  eName                        NVARCHAR(157) COLLATE database_default NOT NULL,
  anyContainer_owner_id        SMALLINT                               NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  dateValue                    DATETIME2                              NOT NULL,
  extensionType                INT,
  dynamicDef                   BIT,
  eType                        NVARCHAR(157) COLLATE database_default,
  valueType                    INT,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
);

CREATE TABLE m_assignment_ext_long (
  eName                        NVARCHAR(157) COLLATE database_default NOT NULL,
  anyContainer_owner_id        SMALLINT                               NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  longValue                    BIGINT                                 NOT NULL,
  extensionType                INT,
  dynamicDef                   BIT,
  eType                        NVARCHAR(157) COLLATE database_default,
  valueType                    INT,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
);

CREATE TABLE m_assignment_ext_poly (
  eName                        NVARCHAR(157) COLLATE database_default NOT NULL,
  anyContainer_owner_id        SMALLINT                               NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  orig                         NVARCHAR(255) COLLATE database_default NOT NULL,
  extensionType                INT,
  dynamicDef                   BIT,
  norm                         NVARCHAR(255) COLLATE database_default,
  eType                        NVARCHAR(157) COLLATE database_default,
  valueType                    INT,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
);

CREATE TABLE m_assignment_ext_reference (
  eName                        NVARCHAR(157) COLLATE database_default NOT NULL,
  anyContainer_owner_id        SMALLINT                               NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  targetoid                    NVARCHAR(36) COLLATE database_default  NOT NULL,
  extensionType                INT,
  dynamicDef                   BIT,
  relation                     NVARCHAR(157) COLLATE database_default,
  targetType                   INT,
  eType                        NVARCHAR(157) COLLATE database_default,
  valueType                    INT,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
);

CREATE TABLE m_assignment_ext_string (
  eName                        NVARCHAR(157) COLLATE database_default NOT NULL,
  anyContainer_owner_id        SMALLINT                               NOT NULL,
  anyContainer_owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  stringValue                  NVARCHAR(255) COLLATE database_default NOT NULL,
  extensionType                INT,
  dynamicDef                   BIT,
  eType                        NVARCHAR(157) COLLATE database_default,
  valueType                    INT,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
);

CREATE TABLE m_assignment_extension (
  owner_id        SMALLINT                              NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  datesCount      SMALLINT,
  longsCount      SMALLINT,
  polysCount      SMALLINT,
  referencesCount SMALLINT,
  stringsCount    SMALLINT,
  PRIMARY KEY (owner_id, owner_owner_oid)
);

CREATE TABLE m_assignment_reference (
  reference_type  INT                                    NOT NULL,
  owner_id        SMALLINT                               NOT NULL,
  owner_owner_oid NVARCHAR(36) COLLATE database_default  NOT NULL,
  relation        NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid       NVARCHAR(36) COLLATE database_default  NOT NULL,
  containerType   INT,
  PRIMARY KEY (owner_id, owner_owner_oid, relation, targetOid)
);

CREATE TABLE m_audit_delta (
  checksum   NVARCHAR(32) COLLATE database_default NOT NULL,
  record_id  BIGINT                                NOT NULL,
  delta      NVARCHAR(MAX),
  deltaOid   NVARCHAR(36) COLLATE database_default,
  deltaType  INT,
  fullResult NVARCHAR(MAX),
  status     INT,
  PRIMARY KEY (checksum, record_id)
);

CREATE TABLE m_audit_event (
  id                BIGINT NOT NULL,
  channel           NVARCHAR(255) COLLATE database_default,
  eventIdentifier   NVARCHAR(255) COLLATE database_default,
  eventStage        INT,
  eventType         INT,
  hostIdentifier    NVARCHAR(255) COLLATE database_default,
  initiatorName     NVARCHAR(255) COLLATE database_default,
  initiatorOid      NVARCHAR(36) COLLATE database_default,
  message           NVARCHAR(1024) COLLATE database_default,
  outcome           INT,
  parameter         NVARCHAR(255) COLLATE database_default,
  result            NVARCHAR(255) COLLATE database_default,
  sessionIdentifier NVARCHAR(255) COLLATE database_default,
  targetName        NVARCHAR(255) COLLATE database_default,
  targetOid         NVARCHAR(36) COLLATE database_default,
  targetOwnerName   NVARCHAR(255) COLLATE database_default,
  targetOwnerOid    NVARCHAR(36) COLLATE database_default,
  targetType        INT,
  taskIdentifier    NVARCHAR(255) COLLATE database_default,
  taskOID           NVARCHAR(255) COLLATE database_default,
  timestampValue    DATETIME2,
  PRIMARY KEY (id)
);

CREATE TABLE m_connector (
  connectorBundle            NVARCHAR(255) COLLATE database_default,
  connectorHostRef_relation  NVARCHAR(157) COLLATE database_default,
  connectorHostRef_targetOid NVARCHAR(36) COLLATE database_default,
  connectorHostRef_type      INT,
  connectorType              NVARCHAR(255) COLLATE database_default,
  connectorVersion           NVARCHAR(255) COLLATE database_default,
  framework                  NVARCHAR(255) COLLATE database_default,
  name_norm                  NVARCHAR(255) COLLATE database_default,
  name_orig                  NVARCHAR(255) COLLATE database_default,
  oid                        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_connector_host (
  hostname  NVARCHAR(255) COLLATE database_default,
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  port      NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_connector_target_system (
  connector_oid    NVARCHAR(36) COLLATE database_default NOT NULL,
  targetSystemType NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_exclusion (
  id                  SMALLINT                              NOT NULL,
  owner_oid           NVARCHAR(36) COLLATE database_default NOT NULL,
  policy              INT,
  targetRef_relation  NVARCHAR(157) COLLATE database_default,
  targetRef_targetOid NVARCHAR(36) COLLATE database_default,
  targetRef_type      INT,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_focus (
  administrativeStatus    INT,
  archiveTimestamp        DATETIME2,
  disableReason           NVARCHAR(255) COLLATE database_default,
  disableTimestamp        DATETIME2,
  effectiveStatus         INT,
  enableTimestamp         DATETIME2,
  validFrom               DATETIME2,
  validTo                 DATETIME2,
  validityChangeTimestamp DATETIME2,
  validityStatus          INT,
  oid                     NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_generic_object (
  name_norm  NVARCHAR(255) COLLATE database_default,
  name_orig  NVARCHAR(255) COLLATE database_default,
  objectType NVARCHAR(255) COLLATE database_default,
  oid        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_node (
  name_norm      NVARCHAR(255) COLLATE database_default,
  name_orig      NVARCHAR(255) COLLATE database_default,
  nodeIdentifier NVARCHAR(255) COLLATE database_default,
  oid            NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_object (
  oid                   NVARCHAR(36) COLLATE database_default NOT NULL,
  createChannel         NVARCHAR(255) COLLATE database_default,
  createTimestamp       DATETIME2,
  creatorRef_relation   NVARCHAR(157) COLLATE database_default,
  creatorRef_targetOid  NVARCHAR(36) COLLATE database_default,
  creatorRef_type       INT,
  datesCount            SMALLINT,
  fullObject            VARBINARY(MAX),
  longsCount            SMALLINT,
  modifierRef_relation  NVARCHAR(157) COLLATE database_default,
  modifierRef_targetOid NVARCHAR(36) COLLATE database_default,
  modifierRef_type      INT,
  modifyChannel         NVARCHAR(255) COLLATE database_default,
  modifyTimestamp       DATETIME2,
  name_norm             NVARCHAR(255) COLLATE database_default,
  name_orig             NVARCHAR(255) COLLATE database_default,
  objectTypeClass       INT,
  polysCount            SMALLINT,
  referencesCount       SMALLINT,
  stringsCount          SMALLINT,
  tenantRef_relation    NVARCHAR(157) COLLATE database_default,
  tenantRef_targetOid   NVARCHAR(36) COLLATE database_default,
  tenantRef_type        INT,
  version               INT                                   NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_object_ext_date (
  eName      NVARCHAR(157) COLLATE database_default NOT NULL,
  owner_oid  NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType  INT                                    NOT NULL,
  dateValue  DATETIME2                              NOT NULL,
  dynamicDef BIT,
  eType      NVARCHAR(157) COLLATE database_default,
  valueType  INT,
  PRIMARY KEY (eName, owner_oid, ownerType, dateValue)
);

CREATE TABLE m_object_ext_long (
  eName      NVARCHAR(157) COLLATE database_default NOT NULL,
  owner_oid  NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType  INT                                    NOT NULL,
  longValue  BIGINT                                 NOT NULL,
  dynamicDef BIT,
  eType      NVARCHAR(157) COLLATE database_default,
  valueType  INT,
  PRIMARY KEY (eName, owner_oid, ownerType, longValue)
);

CREATE TABLE m_object_ext_poly (
  eName      NVARCHAR(157) COLLATE database_default NOT NULL,
  owner_oid  NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType  INT                                    NOT NULL,
  orig       NVARCHAR(255) COLLATE database_default NOT NULL,
  dynamicDef BIT,
  norm       NVARCHAR(255) COLLATE database_default,
  eType      NVARCHAR(157) COLLATE database_default,
  valueType  INT,
  PRIMARY KEY (eName, owner_oid, ownerType, orig)
);

CREATE TABLE m_object_ext_reference (
  eName      NVARCHAR(157) COLLATE database_default NOT NULL,
  owner_oid  NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType  INT                                    NOT NULL,
  targetoid  NVARCHAR(36) COLLATE database_default  NOT NULL,
  dynamicDef BIT,
  relation   NVARCHAR(157) COLLATE database_default,
  targetType INT,
  eType      NVARCHAR(157) COLLATE database_default,
  valueType  INT,
  PRIMARY KEY (eName, owner_oid, ownerType, targetoid)
);

CREATE TABLE m_object_ext_string (
  eName       NVARCHAR(157) COLLATE database_default NOT NULL,
  owner_oid   NVARCHAR(36) COLLATE database_default  NOT NULL,
  ownerType   INT                                    NOT NULL,
  stringValue NVARCHAR(255) COLLATE database_default NOT NULL,
  dynamicDef  BIT,
  eType       NVARCHAR(157) COLLATE database_default,
  valueType   INT,
  PRIMARY KEY (eName, owner_oid, ownerType, stringValue)
);

CREATE TABLE m_object_template (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  type      INT,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_org (
  costCenter       NVARCHAR(255) COLLATE database_default,
  displayName_norm NVARCHAR(255) COLLATE database_default,
  displayName_orig NVARCHAR(255) COLLATE database_default,
  displayOrder     INT,
  identifier       NVARCHAR(255) COLLATE database_default,
  locality_norm    NVARCHAR(255) COLLATE database_default,
  locality_orig    NVARCHAR(255) COLLATE database_default,
  name_norm        NVARCHAR(255) COLLATE database_default,
  name_orig        NVARCHAR(255) COLLATE database_default,
  tenant           BIT,
  oid              NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_org_closure (
  id             BIGINT NOT NULL,
  ancestor_oid   NVARCHAR(36) COLLATE database_default,
  depthValue     INT,
  descendant_oid NVARCHAR(36) COLLATE database_default,
  relation       NVARCHAR(157) COLLATE database_default,
  PRIMARY KEY (id)
);

CREATE TABLE m_org_incorrect (
  descendant_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  ancestor_oid   NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
);

CREATE TABLE m_org_org_type (
  org_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  orgType NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_reference (
  reference_type INT                                    NOT NULL,
  owner_oid      NVARCHAR(36) COLLATE database_default  NOT NULL,
  relation       NVARCHAR(157) COLLATE database_default NOT NULL,
  targetOid      NVARCHAR(36) COLLATE database_default  NOT NULL,
  containerType  INT,
  PRIMARY KEY (owner_oid, relation, targetOid)
);

CREATE TABLE m_report (
  export              INT,
  name_norm           NVARCHAR(255) COLLATE database_default,
  name_orig           NVARCHAR(255) COLLATE database_default,
  orientation         INT,
  parent              BIT,
  useHibernateSession BIT,
  oid                 NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_report_output (
  name_norm           NVARCHAR(255) COLLATE database_default,
  name_orig           NVARCHAR(255) COLLATE database_default,
  reportRef_relation  NVARCHAR(157) COLLATE database_default,
  reportRef_targetOid NVARCHAR(36) COLLATE database_default,
  reportRef_type      INT,
  oid                 NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_resource (
  administrativeState        INT,
  connectorRef_relation      NVARCHAR(157) COLLATE database_default,
  connectorRef_targetOid     NVARCHAR(36) COLLATE database_default,
  connectorRef_type          INT,
  name_norm                  NVARCHAR(255) COLLATE database_default,
  name_orig                  NVARCHAR(255) COLLATE database_default,
  o16_lastAvailabilityStatus INT,
  oid                        NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_role (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  roleType  NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_security_policy (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_shadow (
  attemptNumber                INT,
  dead                         BIT,
  exist                        BIT,
  failedOperationType          INT,
  fullSynchronizationTimestamp DATETIME2,
  intent                       NVARCHAR(255) COLLATE database_default,
  kind                         INT,
  name_norm                    NVARCHAR(255) COLLATE database_default,
  name_orig                    NVARCHAR(255) COLLATE database_default,
  objectClass                  NVARCHAR(157) COLLATE database_default,
  resourceRef_relation         NVARCHAR(157) COLLATE database_default,
  resourceRef_targetOid        NVARCHAR(36) COLLATE database_default,
  resourceRef_type             INT,
  status                       INT,
  synchronizationSituation     INT,
  synchronizationTimestamp     DATETIME2,
  oid                          NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_system_configuration (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_task (
  binding                INT,
  canRunOnNode           NVARCHAR(255) COLLATE database_default,
  category               NVARCHAR(255) COLLATE database_default,
  completionTimestamp    DATETIME2,
  executionStatus        INT,
  handlerUri             NVARCHAR(255) COLLATE database_default,
  lastRunFinishTimestamp DATETIME2,
  lastRunStartTimestamp  DATETIME2,
  name_norm              NVARCHAR(255) COLLATE database_default,
  name_orig              NVARCHAR(255) COLLATE database_default,
  node                   NVARCHAR(255) COLLATE database_default,
  objectRef_relation     NVARCHAR(157) COLLATE database_default,
  objectRef_targetOid    NVARCHAR(36) COLLATE database_default,
  objectRef_type         INT,
  ownerRef_relation      NVARCHAR(157) COLLATE database_default,
  ownerRef_targetOid     NVARCHAR(36) COLLATE database_default,
  ownerRef_type          INT,
  parent                 NVARCHAR(255) COLLATE database_default,
  recurrence             INT,
  status                 INT,
  taskIdentifier         NVARCHAR(255) COLLATE database_default,
  threadStopAction       INT,
  waitingReason          INT,
  oid                    NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE m_task_dependent (
  task_oid  NVARCHAR(36) COLLATE database_default NOT NULL,
  dependent NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_trigger (
  id             SMALLINT                              NOT NULL,
  owner_oid      NVARCHAR(36) COLLATE database_default NOT NULL,
  handlerUri     NVARCHAR(255) COLLATE database_default,
  timestampValue DATETIME2,
  PRIMARY KEY (id, owner_oid)
);

CREATE TABLE m_user (
  additionalName_norm  NVARCHAR(255) COLLATE database_default,
  additionalName_orig  NVARCHAR(255) COLLATE database_default,
  costCenter           NVARCHAR(255) COLLATE database_default,
  emailAddress         NVARCHAR(255) COLLATE database_default,
  employeeNumber       NVARCHAR(255) COLLATE database_default,
  familyName_norm      NVARCHAR(255) COLLATE database_default,
  familyName_orig      NVARCHAR(255) COLLATE database_default,
  fullName_norm        NVARCHAR(255) COLLATE database_default,
  fullName_orig        NVARCHAR(255) COLLATE database_default,
  givenName_norm       NVARCHAR(255) COLLATE database_default,
  givenName_orig       NVARCHAR(255) COLLATE database_default,
  hasPhoto             BIT                                   NOT NULL,
  honorificPrefix_norm NVARCHAR(255) COLLATE database_default,
  honorificPrefix_orig NVARCHAR(255) COLLATE database_default,
  honorificSuffix_norm NVARCHAR(255) COLLATE database_default,
  honorificSuffix_orig NVARCHAR(255) COLLATE database_default,
  locale               NVARCHAR(255) COLLATE database_default,
  locality_norm        NVARCHAR(255) COLLATE database_default,
  locality_orig        NVARCHAR(255) COLLATE database_default,
  name_norm            NVARCHAR(255) COLLATE database_default,
  name_orig            NVARCHAR(255) COLLATE database_default,
  nickName_norm        NVARCHAR(255) COLLATE database_default,
  nickName_orig        NVARCHAR(255) COLLATE database_default,
  preferredLanguage    NVARCHAR(255) COLLATE database_default,
  status               INT,
  telephoneNumber      NVARCHAR(255) COLLATE database_default,
  timezone             NVARCHAR(255) COLLATE database_default,
  title_norm           NVARCHAR(255) COLLATE database_default,
  title_orig           NVARCHAR(255) COLLATE database_default,
  oid                  NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE TABLE m_user_employee_type (
  user_oid     NVARCHAR(36) COLLATE database_default NOT NULL,
  employeeType NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_user_organization (
  user_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  norm     NVARCHAR(255) COLLATE database_default,
  orig     NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_user_organizational_unit (
  user_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  norm     NVARCHAR(255) COLLATE database_default,
  orig     NVARCHAR(255) COLLATE database_default
);

CREATE TABLE m_user_photo (
  owner_oid NVARCHAR(36) COLLATE database_default NOT NULL,
  photo     VARBINARY(MAX),
  PRIMARY KEY (owner_oid)
);

CREATE TABLE m_value_policy (
  name_norm NVARCHAR(255) COLLATE database_default,
  name_orig NVARCHAR(255) COLLATE database_default,
  oid       NVARCHAR(36) COLLATE database_default NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
);

CREATE INDEX iRequestable ON m_abstract_role (requestable);

ALTER TABLE m_abstract_role
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (oid)
REFERENCES m_focus;

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

ALTER TABLE m_assignment
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue);

ALTER TABLE m_assignment_ext_date
ADD CONSTRAINT fk_assignment_ext_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue);

ALTER TABLE m_assignment_ext_long
ADD CONSTRAINT fk_assignment_ext_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig);

ALTER TABLE m_assignment_ext_poly
ADD CONSTRAINT fk_assignment_ext_poly
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid);

ALTER TABLE m_assignment_ext_reference
ADD CONSTRAINT fk_assignment_ext_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue);

ALTER TABLE m_assignment_ext_string
ADD CONSTRAINT fk_assignment_ext_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension;

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid);

ALTER TABLE m_assignment_reference
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment;

ALTER TABLE m_audit_delta
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event;

ALTER TABLE m_connector
ADD CONSTRAINT fk_connector
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_connector_host
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_connector_target_system
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_oid)
REFERENCES m_connector;

ALTER TABLE m_exclusion
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_focus
ADD CONSTRAINT fk_focus
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_generic_object
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_node
ADD CONSTRAINT fk_node
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE INDEX iObjectTypeClass ON m_object (objectTypeClass);

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);

CREATE INDEX iExtensionDate ON m_object_ext_date (ownerType, eName, dateValue);

CREATE INDEX iExtensionDateDef ON m_object_ext_date (owner_oid, ownerType);

ALTER TABLE m_object_ext_date
ADD CONSTRAINT fk_object_ext_date
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iExtensionLong ON m_object_ext_long (ownerType, eName, longValue);

CREATE INDEX iExtensionLongDef ON m_object_ext_long (owner_oid, ownerType);

ALTER TABLE m_object_ext_long
ADD CONSTRAINT fk_object_ext_long
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iExtensionPolyString ON m_object_ext_poly (ownerType, eName, orig);

CREATE INDEX iExtensionPolyStringDef ON m_object_ext_poly (owner_oid, ownerType);

ALTER TABLE m_object_ext_poly
ADD CONSTRAINT fk_object_ext_poly
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iExtensionReference ON m_object_ext_reference (ownerType, eName, targetoid);

CREATE INDEX iExtensionReferenceDef ON m_object_ext_reference (owner_oid, ownerType);

ALTER TABLE m_object_ext_reference
ADD CONSTRAINT fk_object_ext_reference
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iExtensionString ON m_object_ext_string (ownerType, eName, stringValue);

CREATE INDEX iExtensionStringDef ON m_object_ext_string (owner_oid, ownerType);

ALTER TABLE m_object_ext_string
ADD CONSTRAINT fk_object_ext_string
FOREIGN KEY (owner_oid)
REFERENCES m_object;

ALTER TABLE m_object_template
ADD CONSTRAINT fk_object_template
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE INDEX iDisplayOrder ON m_org (displayOrder);

ALTER TABLE m_org
ADD CONSTRAINT fk_org
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

CREATE INDEX iAncestor ON m_org_closure (ancestor_oid);

CREATE INDEX iDescendant ON m_org_closure (descendant_oid);

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_oid, depthValue);

CREATE INDEX iAncDescDepth ON m_org_closure (ancestor_oid, descendant_oid, depthValue);

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object;

ALTER TABLE m_org_closure
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object;

ALTER TABLE m_org_org_type
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_oid)
REFERENCES m_org;

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid);

ALTER TABLE m_reference
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iReportParent ON m_report (parent);

ALTER TABLE m_report
ADD CONSTRAINT fk_report
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_report_output
ADD CONSTRAINT fk_report_output
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_resource
ADD CONSTRAINT fk_resource
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_role
ADD CONSTRAINT fk_role
FOREIGN KEY (oid)
REFERENCES m_abstract_role;

ALTER TABLE m_security_policy
ADD CONSTRAINT fk_security_policy
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowDead ON m_shadow (dead);

ALTER TABLE m_shadow
ADD CONSTRAINT fk_shadow
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_system_configuration
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_task
ADD CONSTRAINT fk_task
FOREIGN KEY (oid)
REFERENCES m_object;

ALTER TABLE m_task_dependent
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_oid)
REFERENCES m_task;

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_trigger
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object;

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iFullName ON m_user (fullName_orig);

CREATE INDEX iFamilyName ON m_user (familyName_orig);

CREATE INDEX iGivenName ON m_user (givenName_orig);

CREATE INDEX iLocality ON m_user (locality_orig);

ALTER TABLE m_user
ADD CONSTRAINT fk_user
FOREIGN KEY (oid)
REFERENCES m_focus;

ALTER TABLE m_user_employee_type
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organization
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_user_organizational_unit
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_oid)
REFERENCES m_user;

ALTER TABLE m_user_photo
ADD CONSTRAINT fk_user_photo
FOREIGN KEY (owner_oid)
REFERENCES m_user;

ALTER TABLE m_value_policy
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (oid)
REFERENCES m_object;

CREATE TABLE hibernate_sequence (
  next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);
