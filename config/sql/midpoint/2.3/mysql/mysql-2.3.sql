# use for db create
# CREATE DATABASE <database name>
#   CHARACTER SET utf8
#   DEFAULT CHARACTER SET utf8
#   COLLATE utf8_bin
#   DEFAULT COLLATE utf8_bin
# ;

# replace "ENGINE=InnoDB" with "DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ENGINE=InnoDB"
# replace "DATETIME" with "DATETIME(6)"

# remove iAncestor and iDescendant index, they are the same as FK for that fields

CREATE TABLE m_abstract_role (
  approvalProcess VARCHAR(255),
  requestable     BIT,
  oid             VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment (
  id                      SMALLINT    NOT NULL,
  owner_oid               VARCHAR(36) NOT NULL,
  administrativeStatus    INTEGER,
  archiveTimestamp        DATETIME(6),
  disableReason           VARCHAR(255),
  disableTimestamp        DATETIME(6),
  effectiveStatus         INTEGER,
  enableTimestamp         DATETIME(6),
  validFrom               DATETIME(6),
  validTo                 DATETIME(6),
  validityChangeTimestamp DATETIME(6),
  validityStatus          INTEGER,
  assignmentOwner         INTEGER,
  createChannel           VARCHAR(255),
  createTimestamp         DATETIME(6),
  creatorRef_relation     VARCHAR(157),
  creatorRef_targetOid    VARCHAR(36),
  creatorRef_type         INTEGER,
  modifierRef_relation    VARCHAR(157),
  modifierRef_targetOid   VARCHAR(36),
  modifierRef_type        INTEGER,
  modifyChannel           VARCHAR(255),
  modifyTimestamp         DATETIME(6),
  orderValue              INTEGER,
  targetRef_relation      VARCHAR(157),
  targetRef_targetOid     VARCHAR(36),
  targetRef_type          INTEGER,
  tenantRef_relation      VARCHAR(157),
  tenantRef_targetOid     VARCHAR(36),
  tenantRef_type          INTEGER,
  extId                   SMALLINT,
  extOid                  VARCHAR(36),
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_date (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  dateValue                    DATETIME(6)  NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_long (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  longValue                    BIGINT       NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_poly (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  orig                         VARCHAR(255) NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  norm                         VARCHAR(255),
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_reference (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  targetoid                    VARCHAR(36)  NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  relation                     VARCHAR(157),
  targetType                   INTEGER,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_ext_string (
  eName                        VARCHAR(157) NOT NULL,
  anyContainer_owner_id        SMALLINT     NOT NULL,
  anyContainer_owner_owner_oid VARCHAR(36)  NOT NULL,
  stringValue                  VARCHAR(255) NOT NULL,
  extensionType                INTEGER,
  dynamicDef                   BIT,
  eType                        VARCHAR(157),
  valueType                    INTEGER,
  PRIMARY KEY (eName, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_extension (
  owner_id        SMALLINT    NOT NULL,
  owner_owner_oid VARCHAR(36) NOT NULL,
  datesCount      SMALLINT,
  longsCount      SMALLINT,
  polysCount      SMALLINT,
  referencesCount SMALLINT,
  stringsCount    SMALLINT,
  PRIMARY KEY (owner_id, owner_owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_assignment_reference (
  reference_type  INTEGER      NOT NULL,
  owner_id        SMALLINT     NOT NULL,
  owner_owner_oid VARCHAR(36)  NOT NULL,
  relation        VARCHAR(157) NOT NULL,
  targetOid       VARCHAR(36)  NOT NULL,
  containerType   INTEGER,
  PRIMARY KEY (owner_id, owner_owner_oid, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_audit_delta (
  checksum   VARCHAR(32) NOT NULL,
  record_id  BIGINT      NOT NULL,
  delta      LONGTEXT,
  deltaOid   VARCHAR(36),
  deltaType  INTEGER,
  fullResult LONGTEXT,
  status     INTEGER,
  PRIMARY KEY (checksum, record_id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_audit_event (
  id                BIGINT NOT NULL,
  channel           VARCHAR(255),
  eventIdentifier   VARCHAR(255),
  eventStage        INTEGER,
  eventType         INTEGER,
  hostIdentifier    VARCHAR(255),
  initiatorName     VARCHAR(255),
  initiatorOid      VARCHAR(36),
  message           VARCHAR(1024),
  outcome           INTEGER,
  parameter         VARCHAR(255),
  result            VARCHAR(255),
  sessionIdentifier VARCHAR(255),
  targetName        VARCHAR(255),
  targetOid         VARCHAR(36),
  targetOwnerName   VARCHAR(255),
  targetOwnerOid    VARCHAR(36),
  targetType        INTEGER,
  taskIdentifier    VARCHAR(255),
  taskOID           VARCHAR(255),
  timestampValue    DATETIME(6),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector (
  connectorBundle            VARCHAR(255),
  connectorHostRef_relation  VARCHAR(157),
  connectorHostRef_targetOid VARCHAR(36),
  connectorHostRef_type      INTEGER,
  connectorType              VARCHAR(255),
  connectorVersion           VARCHAR(255),
  framework                  VARCHAR(255),
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector_host (
  hostname  VARCHAR(255),
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  port      VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_connector_target_system (
  connector_oid    VARCHAR(36) NOT NULL,
  targetSystemType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_exclusion (
  id                  SMALLINT    NOT NULL,
  owner_oid           VARCHAR(36) NOT NULL,
  policy              INTEGER,
  targetRef_relation  VARCHAR(157),
  targetRef_targetOid VARCHAR(36),
  targetRef_type      INTEGER,
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_focus (
  administrativeStatus    INTEGER,
  archiveTimestamp        DATETIME(6),
  disableReason           VARCHAR(255),
  disableTimestamp        DATETIME(6),
  effectiveStatus         INTEGER,
  enableTimestamp         DATETIME(6),
  validFrom               DATETIME(6),
  validTo                 DATETIME(6),
  validityChangeTimestamp DATETIME(6),
  validityStatus          INTEGER,
  oid                     VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_generic_object (
  name_norm  VARCHAR(255),
  name_orig  VARCHAR(255),
  objectType VARCHAR(255),
  oid        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_node (
  name_norm      VARCHAR(255),
  name_orig      VARCHAR(255),
  nodeIdentifier VARCHAR(255),
  oid            VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object (
  oid                   VARCHAR(36) NOT NULL,
  createChannel         VARCHAR(255),
  createTimestamp       DATETIME(6),
  creatorRef_relation   VARCHAR(157),
  creatorRef_targetOid  VARCHAR(36),
  creatorRef_type       INTEGER,
  datesCount            SMALLINT,
  fullObject            LONGBLOB,
  longsCount            SMALLINT,
  modifierRef_relation  VARCHAR(157),
  modifierRef_targetOid VARCHAR(36),
  modifierRef_type      INTEGER,
  modifyChannel         VARCHAR(255),
  modifyTimestamp       DATETIME(6),
  name_norm             VARCHAR(255),
  name_orig             VARCHAR(255),
  polysCount            SMALLINT,
  referencesCount       SMALLINT,
  stringsCount          SMALLINT,
  tenantRef_relation    VARCHAR(157),
  tenantRef_targetOid   VARCHAR(36),
  tenantRef_type        INTEGER,
  version               INTEGER     NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_date (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  dateValue  DATETIME(6)  NOT NULL,
  dynamicDef BIT,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, dateValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_long (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  longValue  BIGINT       NOT NULL,
  dynamicDef BIT,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, longValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_poly (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  orig       VARCHAR(255) NOT NULL,
  dynamicDef BIT,
  norm       VARCHAR(255),
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, orig)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_reference (
  eName      VARCHAR(157) NOT NULL,
  owner_oid  VARCHAR(36)  NOT NULL,
  ownerType  INTEGER      NOT NULL,
  targetoid  VARCHAR(36)  NOT NULL,
  dynamicDef BIT,
  relation   VARCHAR(157),
  targetType INTEGER,
  eType      VARCHAR(157),
  valueType  INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, targetoid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_ext_string (
  eName       VARCHAR(157) NOT NULL,
  owner_oid   VARCHAR(36)  NOT NULL,
  ownerType   INTEGER      NOT NULL,
  stringValue VARCHAR(255) NOT NULL,
  dynamicDef  BIT,
  eType       VARCHAR(157),
  valueType   INTEGER,
  PRIMARY KEY (eName, owner_oid, ownerType, stringValue)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_object_template (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  type      INTEGER,
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org (
  costCenter       VARCHAR(255),
  displayName_norm VARCHAR(255),
  displayName_orig VARCHAR(255),
  identifier       VARCHAR(255),
  locality_norm    VARCHAR(255),
  locality_orig    VARCHAR(255),
  name_norm        VARCHAR(255),
  name_orig        VARCHAR(255),
  tenant           BIT,
  oid              VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_closure (
  id             BIGINT NOT NULL,
  ancestor_oid   VARCHAR(36),
  depthValue     INTEGER,
  descendant_oid VARCHAR(36),
  relation       VARCHAR(157),
  PRIMARY KEY (id)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_incorrect (
  descendant_oid VARCHAR(36) NOT NULL,
  ancestor_oid   VARCHAR(36) NOT NULL,
  PRIMARY KEY (descendant_oid, ancestor_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_org_org_type (
  org_oid VARCHAR(36) NOT NULL,
  orgType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_reference (
  reference_type INTEGER      NOT NULL,
  owner_oid      VARCHAR(36)  NOT NULL,
  relation       VARCHAR(157) NOT NULL,
  targetOid      VARCHAR(36)  NOT NULL,
  containerType  INTEGER,
  PRIMARY KEY (owner_oid, relation, targetOid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_report (
  export                   INTEGER,
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  orientation              INTEGER,
  parent                   BIT,
  useHibernateSession      BIT,
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_report_output (
  name_norm           VARCHAR(255),
  name_orig           VARCHAR(255),
  reportRef_relation  VARCHAR(157),
  reportRef_targetOid VARCHAR(36),
  reportRef_type      INTEGER,
  oid                 VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_resource (
  administrativeState        INTEGER,
  connectorRef_relation      VARCHAR(157),
  connectorRef_targetOid     VARCHAR(36),
  connectorRef_type          INTEGER,
  name_norm                  VARCHAR(255),
  name_orig                  VARCHAR(255),
  o16_lastAvailabilityStatus INTEGER,
  oid                        VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_role (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  roleType  VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_security_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_shadow (
  attemptNumber                INTEGER,
  dead                         BIT,
  exist                        BIT,
  failedOperationType          INTEGER,
  fullSynchronizationTimestamp DATETIME(6),
  intent                       VARCHAR(255),
  kind                         INTEGER,
  name_norm                    VARCHAR(255),
  name_orig                    VARCHAR(255),
  objectClass                  VARCHAR(157),
  resourceRef_relation         VARCHAR(157),
  resourceRef_targetOid        VARCHAR(36),
  resourceRef_type             INTEGER,
  status                       INTEGER,
  synchronizationSituation     INTEGER,
  synchronizationTimestamp     DATETIME(6),
  oid                          VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_system_configuration (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task (
  binding                INTEGER,
  canRunOnNode           VARCHAR(255),
  category               VARCHAR(255),
  completionTimestamp    DATETIME(6),
  executionStatus        INTEGER,
  handlerUri             VARCHAR(255),
  lastRunFinishTimestamp DATETIME(6),
  lastRunStartTimestamp  DATETIME(6),
  name_norm              VARCHAR(255),
  name_orig              VARCHAR(255),
  node                   VARCHAR(255),
  objectRef_relation     VARCHAR(157),
  objectRef_targetOid    VARCHAR(36),
  objectRef_type         INTEGER,
  ownerRef_relation      VARCHAR(157),
  ownerRef_targetOid     VARCHAR(36),
  ownerRef_type          INTEGER,
  parent                 VARCHAR(255),
  recurrence             INTEGER,
  status                 INTEGER,
  taskIdentifier         VARCHAR(255),
  threadStopAction       INTEGER,
  waitingReason          INTEGER,
  oid                    VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_task_dependent (
  task_oid  VARCHAR(36) NOT NULL,
  dependent VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_trigger (
  id             SMALLINT    NOT NULL,
  owner_oid      VARCHAR(36) NOT NULL,
  handlerUri     VARCHAR(255),
  timestampValue DATETIME(6),
  PRIMARY KEY (id, owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user (
  additionalName_norm      VARCHAR(255),
  additionalName_orig      VARCHAR(255),
  costCenter               VARCHAR(255),
  emailAddress             VARCHAR(255),
  employeeNumber           VARCHAR(255),
  familyName_norm          VARCHAR(255),
  familyName_orig          VARCHAR(255),
  fullName_norm            VARCHAR(255),
  fullName_orig            VARCHAR(255),
  givenName_norm           VARCHAR(255),
  givenName_orig           VARCHAR(255),
  hasPhoto                 BIT         NOT NULL,
  honorificPrefix_norm     VARCHAR(255),
  honorificPrefix_orig     VARCHAR(255),
  honorificSuffix_norm     VARCHAR(255),
  honorificSuffix_orig     VARCHAR(255),
  locale                   VARCHAR(255),
  locality_norm            VARCHAR(255),
  locality_orig            VARCHAR(255),
  name_norm                VARCHAR(255),
  name_orig                VARCHAR(255),
  nickName_norm            VARCHAR(255),
  nickName_orig            VARCHAR(255),
  preferredLanguage        VARCHAR(255),
  status                   INTEGER,
  telephoneNumber          VARCHAR(255),
  timezone                 VARCHAR(255),
  title_norm               VARCHAR(255),
  title_orig               VARCHAR(255),
  oid                      VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_employee_type (
  user_oid     VARCHAR(36) NOT NULL,
  employeeType VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organization (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_organizational_unit (
  user_oid VARCHAR(36) NOT NULL,
  norm     VARCHAR(255),
  orig     VARCHAR(255)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_user_photo (
  owner_oid VARCHAR(36) NOT NULL,
  photo     LONGBLOB,
  PRIMARY KEY (owner_oid)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE TABLE m_value_policy (
  name_norm VARCHAR(255),
  name_orig VARCHAR(255),
  oid       VARCHAR(36) NOT NULL,
  PRIMARY KEY (oid),
  UNIQUE (name_norm)
)
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_bin
  ENGINE =InnoDB;

CREATE INDEX iRequestable ON m_abstract_role (requestable);

ALTER TABLE m_abstract_role
ADD INDEX fk_abstract_role (oid),
ADD CONSTRAINT fk_abstract_role
FOREIGN KEY (oid)
REFERENCES m_focus (oid);

CREATE INDEX iAssignmentAdministrative ON m_assignment (administrativeStatus);

CREATE INDEX iAssignmentEffective ON m_assignment (effectiveStatus);

ALTER TABLE m_assignment
ADD INDEX fk_assignment_owner (owner_oid),
ADD CONSTRAINT fk_assignment_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iAExtensionDate ON m_assignment_ext_date (extensionType, eName, dateValue);

ALTER TABLE m_assignment_ext_date
ADD INDEX fk_assignment_ext_date (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_date
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionLong ON m_assignment_ext_long (extensionType, eName, longValue);

ALTER TABLE m_assignment_ext_long
ADD INDEX fk_assignment_ext_long (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_long
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionPolyString ON m_assignment_ext_poly (extensionType, eName, orig);

ALTER TABLE m_assignment_ext_poly
ADD INDEX fk_assignment_ext_poly (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_poly
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionReference ON m_assignment_ext_reference (extensionType, eName, targetoid);

ALTER TABLE m_assignment_ext_reference
ADD INDEX fk_assignment_ext_reference (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_reference
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAExtensionString ON m_assignment_ext_string (extensionType, eName, stringValue);

ALTER TABLE m_assignment_ext_string
ADD INDEX fk_assignment_ext_string (anyContainer_owner_id, anyContainer_owner_owner_oid),
ADD CONSTRAINT fk_assignment_ext_string
FOREIGN KEY (anyContainer_owner_id, anyContainer_owner_owner_oid)
REFERENCES m_assignment_extension (owner_id, owner_owner_oid);

CREATE INDEX iAssignmentReferenceTargetOid ON m_assignment_reference (targetOid);

ALTER TABLE m_assignment_reference
ADD INDEX fk_assignment_reference (owner_id, owner_owner_oid),
ADD CONSTRAINT fk_assignment_reference
FOREIGN KEY (owner_id, owner_owner_oid)
REFERENCES m_assignment (id, owner_oid);

ALTER TABLE m_audit_delta
ADD INDEX fk_audit_delta (record_id),
ADD CONSTRAINT fk_audit_delta
FOREIGN KEY (record_id)
REFERENCES m_audit_event (id);

ALTER TABLE m_connector
ADD INDEX fk_connector (oid),
ADD CONSTRAINT fk_connector
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_connector_host
ADD INDEX fk_connector_host (oid),
ADD CONSTRAINT fk_connector_host
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_connector_target_system
ADD INDEX fk_connector_target_system (connector_oid),
ADD CONSTRAINT fk_connector_target_system
FOREIGN KEY (connector_oid)
REFERENCES m_connector (oid);

ALTER TABLE m_exclusion
ADD INDEX fk_exclusion_owner (owner_oid),
ADD CONSTRAINT fk_exclusion_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iFocusAdministrative ON m_focus (administrativeStatus);

CREATE INDEX iFocusEffective ON m_focus (effectiveStatus);

ALTER TABLE m_focus
ADD INDEX fk_focus (oid),
ADD CONSTRAINT fk_focus
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_generic_object
ADD INDEX fk_generic_object (oid),
ADD CONSTRAINT fk_generic_object
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_node
ADD INDEX fk_node (oid),
ADD CONSTRAINT fk_node
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iObjectNameOrig ON m_object (name_orig);

CREATE INDEX iObjectNameNorm ON m_object (name_norm);

CREATE INDEX iExtensionDate ON m_object_ext_date (ownerType, eName, dateValue);

CREATE INDEX iExtensionDateDef ON m_object_ext_date (owner_oid, ownerType);

ALTER TABLE m_object_ext_date
ADD INDEX fk_object_ext_date (owner_oid),
ADD CONSTRAINT fk_object_ext_date
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionLong ON m_object_ext_long (ownerType, eName, longValue);

CREATE INDEX iExtensionLongDef ON m_object_ext_long (owner_oid, ownerType);

ALTER TABLE m_object_ext_long
ADD INDEX fk_object_ext_long (owner_oid),
ADD CONSTRAINT fk_object_ext_long
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionPolyString ON m_object_ext_poly (ownerType, eName, orig);

CREATE INDEX iExtensionPolyStringDef ON m_object_ext_poly (owner_oid, ownerType);

ALTER TABLE m_object_ext_poly
ADD INDEX fk_object_ext_poly (owner_oid),
ADD CONSTRAINT fk_object_ext_poly
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionReference ON m_object_ext_reference (ownerType, eName, targetoid);

CREATE INDEX iExtensionReferenceDef ON m_object_ext_reference (owner_oid, ownerType);

ALTER TABLE m_object_ext_reference
ADD INDEX fk_object_ext_reference (owner_oid),
ADD CONSTRAINT fk_object_ext_reference
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iExtensionString ON m_object_ext_string (ownerType, eName, stringValue);

CREATE INDEX iExtensionStringDef ON m_object_ext_string (owner_oid, ownerType);

ALTER TABLE m_object_ext_string
ADD INDEX fk_object_ext_string (owner_oid),
ADD CONSTRAINT fk_object_ext_string
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

ALTER TABLE m_object_template
ADD INDEX fk_object_template (oid),
ADD CONSTRAINT fk_object_template
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_org
ADD INDEX fk_org (oid),
ADD CONSTRAINT fk_org
FOREIGN KEY (oid)
REFERENCES m_abstract_role (oid);

CREATE INDEX iAncestorDepth ON m_org_closure (ancestor_oid, depthValue);

CREATE INDEX iAncDescDepth ON m_org_closure (ancestor_oid, descendant_oid, depthValue);

ALTER TABLE m_org_closure
ADD INDEX fk_ancestor (ancestor_oid),
ADD CONSTRAINT fk_ancestor
FOREIGN KEY (ancestor_oid)
REFERENCES m_object (oid);

ALTER TABLE m_org_closure
ADD INDEX fk_descendant (descendant_oid),
ADD CONSTRAINT fk_descendant
FOREIGN KEY (descendant_oid)
REFERENCES m_object (oid);

ALTER TABLE m_org_org_type
ADD INDEX fk_org_org_type (org_oid),
ADD CONSTRAINT fk_org_org_type
FOREIGN KEY (org_oid)
REFERENCES m_org (oid);

CREATE INDEX iReferenceTargetOid ON m_reference (targetOid);

ALTER TABLE m_reference
ADD INDEX fk_reference_owner (owner_oid),
ADD CONSTRAINT fk_reference_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iReportParent ON m_report (parent);

ALTER TABLE m_report
ADD INDEX fk_report (oid),
ADD CONSTRAINT fk_report
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_report_output
ADD INDEX fk_report_output (oid),
ADD CONSTRAINT fk_report_output
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_resource
ADD INDEX fk_resource (oid),
ADD CONSTRAINT fk_resource
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_role
ADD INDEX fk_role (oid),
ADD CONSTRAINT fk_role
FOREIGN KEY (oid)
REFERENCES m_abstract_role (oid);

ALTER TABLE m_security_policy
ADD INDEX fk_security_policy (oid),
ADD CONSTRAINT fk_security_policy
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iShadowResourceRef ON m_shadow (resourceRef_targetOid);

CREATE INDEX iShadowDead ON m_shadow (dead);

ALTER TABLE m_shadow
ADD INDEX fk_shadow (oid),
ADD CONSTRAINT fk_shadow
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_system_configuration
ADD INDEX fk_system_configuration (oid),
ADD CONSTRAINT fk_system_configuration
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE INDEX iParent ON m_task (parent);

ALTER TABLE m_task
ADD INDEX fk_task (oid),
ADD CONSTRAINT fk_task
FOREIGN KEY (oid)
REFERENCES m_object (oid);

ALTER TABLE m_task_dependent
ADD INDEX fk_task_dependent (task_oid),
ADD CONSTRAINT fk_task_dependent
FOREIGN KEY (task_oid)
REFERENCES m_task (oid);

CREATE INDEX iTriggerTimestamp ON m_trigger (timestampValue);

ALTER TABLE m_trigger
ADD INDEX fk_trigger_owner (owner_oid),
ADD CONSTRAINT fk_trigger_owner
FOREIGN KEY (owner_oid)
REFERENCES m_object (oid);

CREATE INDEX iEmployeeNumber ON m_user (employeeNumber);

CREATE INDEX iFullName ON m_user (fullName_orig);

CREATE INDEX iFamilyName ON m_user (familyName_orig);

CREATE INDEX iGivenName ON m_user (givenName_orig);

CREATE INDEX iLocality ON m_user (locality_orig);

ALTER TABLE m_user
ADD INDEX fk_user (oid),
ADD CONSTRAINT fk_user
FOREIGN KEY (oid)
REFERENCES m_focus (oid);

ALTER TABLE m_user_employee_type
ADD INDEX fk_user_employee_type (user_oid),
ADD CONSTRAINT fk_user_employee_type
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_organization
ADD INDEX fk_user_organization (user_oid),
ADD CONSTRAINT fk_user_organization
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_organizational_unit
ADD INDEX fk_user_org_unit (user_oid),
ADD CONSTRAINT fk_user_org_unit
FOREIGN KEY (user_oid)
REFERENCES m_user (oid);

ALTER TABLE m_user_photo
ADD INDEX fk_user_photo (owner_oid),
ADD CONSTRAINT fk_user_photo
FOREIGN KEY (owner_oid)
REFERENCES m_user (oid);

ALTER TABLE m_value_policy
ADD INDEX fk_value_policy (oid),
ADD CONSTRAINT fk_value_policy
FOREIGN KEY (oid)
REFERENCES m_object (oid);

CREATE TABLE hibernate_sequence (
  next_val BIGINT
);

INSERT INTO hibernate_sequence VALUES (1);
