package it.teamdigitale

import java.security.PrivilegedExceptionAction

import org.apache.hadoop.security.UserGroupInformation

package object web {

  implicit class UserGroupInformationSyntax(user: UserGroupInformation) {

    def as[U](otherUserId: String)(action: => U): U = UserGroupInformation.createProxyUser(otherUserId, user).doAs {
      new PrivilegedExceptionAction[U]() { def run: U = action }
    }

  }

}
