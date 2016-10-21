/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.idm.engine.impl;

import java.util.List;

import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;
import org.activiti.idm.api.UserQueryProperty;
import org.activiti.idm.engine.ActivitiIdmIllegalArgumentException;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class UserQueryImpl extends AbstractQuery<UserQuery, User> implements UserQuery {

  private static final long serialVersionUID = 1L;
  protected String id;
  protected List<String> ids;
  protected String firstName;
  protected String firstNameLike;
  protected String lastName;
  protected String lastNameLike;
  protected String fullNameLike;
  protected String email;
  protected String emailLike;
  protected String groupId;
  protected List<String> groupIds;

  public UserQueryImpl() {
  }

  public UserQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public UserQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  public UserQuery userId(String id) {
    if (id == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided id is null");
    }
    this.id = id;
    return this;
  }
  
  public UserQuery userIds(List<String> ids) {
    if (ids == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided ids is null");
    }
    this.ids = ids;
    return this;
  }

  public UserQuery userFirstName(String firstName) {
    if (firstName == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided firstName is null");
    }
    this.firstName = firstName;
    return this;
  }

  public UserQuery userFirstNameLike(String firstNameLike) {
    if (firstNameLike == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided firstNameLike is null");
    }
    this.firstNameLike = firstNameLike;
    return this;
  }

  public UserQuery userLastName(String lastName) {
    if (lastName == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided lastName is null");
    }
    this.lastName = lastName;
    return this;
  }

  public UserQuery userLastNameLike(String lastNameLike) {
    if (lastNameLike == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided lastNameLike is null");
    }
    this.lastNameLike = lastNameLike;
    return this;
  }

  public UserQuery userFullNameLike(String fullNameLike) {
    if (fullNameLike == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided full name is null");
    }
    this.fullNameLike = fullNameLike;
    return this;
  }

  public UserQuery userEmail(String email) {
    if (email == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided email is null");
    }
    this.email = email;
    return this;
  }

  public UserQuery userEmailLike(String emailLike) {
    if (emailLike == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided emailLike is null");
    }
    this.emailLike = emailLike;
    return this;
  }

  public UserQuery memberOfGroup(String groupId) {
    if (groupId == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided groupId is null");
    }
    this.groupId = groupId;
    return this;
  }
  
  public UserQuery memberOfGroups(List<String> groupIds) {
    if (groupIds == null) {
      throw new ActivitiIdmIllegalArgumentException("Provided groupIds is null");
    }
    this.groupIds = groupIds;
    return this;
  }

  // sorting //////////////////////////////////////////////////////////

  public UserQuery orderByUserId() {
    return orderBy(UserQueryProperty.USER_ID);
  }

  public UserQuery orderByUserEmail() {
    return orderBy(UserQueryProperty.EMAIL);
  }

  public UserQuery orderByUserFirstName() {
    return orderBy(UserQueryProperty.FIRST_NAME);
  }

  public UserQuery orderByUserLastName() {
    return orderBy(UserQueryProperty.LAST_NAME);
  }

  // results //////////////////////////////////////////////////////////

  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext.getUserEntityManager().findUserCountByQueryCriteria(this);
  }

  public List<User> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext.getUserEntityManager().findUserByQueryCriteria(this, page);
  }

  // getters //////////////////////////////////////////////////////////

  public String getId() {
    return id;
  }

  public List<String> getIds() {
    return ids;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getFirstNameLike() {
    return firstNameLike;
  }

  public String getLastName() {
    return lastName;
  }

  public String getLastNameLike() {
    return lastNameLike;
  }

  public String getEmail() {
    return email;
  }

  public String getEmailLike() {
    return emailLike;
  }

  public String getGroupId() {
    return groupId;
  }
  
  public List<String> getGroupIds() {
    return groupIds;
  }

  public String getFullNameLike() {
    return fullNameLike;
  }
  
}