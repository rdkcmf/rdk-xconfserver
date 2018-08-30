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
 * Author: slavrenyuk
 * Created: 5/21/14
 */
package com.comcast.hesperius.dataaccess.core.dao.util.dbclient;

/**
 * Following retry policies are supported:
 * INDEFINITE - corresponds to {@link com.netflix.astyanax.retry.IndefiniteRetry} class,
 * RETRY_NTIMES - corresponds to {@link com.netflix.astyanax.retry.RetryNTimes} class,
 * RUN_ONCE - corresponds to {@link com.netflix.astyanax.retry.RunOnce} class,
 * CONSTANT_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.ConstantBackoff} class,
 * EXPONENTIAL_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.ExponentialBackoff} class,
 * BOUNDED_EXPONENTIAL_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.BoundedExponentialBackoff} class
 */
public enum RetryPolicyType {
    INDEFINITE,
    RETRY_NTIMES,
    RUN_ONCE,
    CONSTANT_BACKOFF,
    EXPONENTIAL_BACKOFF,
    BOUNDED_EXPONENTIAL_BACKOFF
}
