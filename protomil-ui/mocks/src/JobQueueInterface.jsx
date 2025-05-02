import { useState } from 'react';
import { Search, Filter, Calendar, Clock, Settings, ChevronRight, ChevronLeft, User, MapPin } from 'lucide-react';

export default function StackedJobQueueInterface() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [activeFilter, setActiveFilter] = useState('all');
  
  // Technician profile
  const tech = {
    name: "Michael Rodriguez",
    title: "Senior HVAC Technician",
    id: "TECH-387",
    stats: { total: 5, completed: 1 }
  };

  // Sample job data
  const jobs = [
    {
      id: 'JOB-2025-0431',
      title: 'Emergency AC Repair',
      customer: 'Sunrise Senior Living',
      address: '2200 Eldercare Way',
      priority: 'urgent',
      dueDate: 'Today, 1:00 PM',
      desc: 'AC failure in east wing. Senior residence facility requires immediate attention.',
      time: '2-5 hrs'
    },
    {
      id: 'JOB-2025-0428',
      title: 'HVAC System Repair',
      customer: 'Riverdale Apartments',
      address: '1250 Parkview Ave, Building C',
      priority: 'high',
      dueDate: 'Today, 2:00 PM',
      desc: 'Multiple tenant complaints about insufficient cooling in common areas.',
      time: '3-4 hrs'
    },
    {
      id: 'JOB-2025-0429',
      title: 'Quarterly Maintenance',
      customer: 'Summit Office Complex',
      address: '875 Business Park Drive',
      priority: 'medium',
      dueDate: 'Today, 5:00 PM',
      desc: 'Scheduled quarterly maintenance of all lobby HVAC units.',
      time: '2-3 hrs'
    },
    {
      id: 'JOB-2025-0430',
      title: 'Thermostat Replacement',
      customer: 'Greenfield Residence',
      address: '45 Oakwood Lane',
      priority: 'medium',
      dueDate: 'Tomorrow, 11:00 AM',
      desc: 'Customer reports thermostat unresponsive. Bring replacement units.',
      time: '1 hr'
    },
    {
      id: 'JOB-2025-0432',
      title: 'New System Assessment',
      customer: 'Highland Restaurant',
      address: '890 Culinary Blvd',
      priority: 'low',
      dueDate: 'Tomorrow, 3:00 PM',
      desc: 'Customer interested in upgrading kitchen ventilation system.',
      time: '2 hrs'
    }
  ];

  // Filter and sort jobs
  const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
  const filteredJobs = activeFilter === 'all' 
    ? [...jobs].sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority])
    : [...jobs].filter(job => job.priority === activeFilter)
                .sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority]);

  // Handle navigation
  const nextJob = () => {
    setActiveIndex((prevIndex) => (prevIndex + 1) % filteredJobs.length);
  };

  const prevJob = () => {
    setActiveIndex((prevIndex) => (prevIndex - 1 + filteredJobs.length) % filteredJobs.length);
  };

  // Helper functions for styling
  const getPriorityColor = (priority) => {
    const colors = {
      urgent: 'bg-red-500',
      high: 'bg-orange-500',
      medium: 'bg-blue-500',
      low: 'bg-green-500'
    };
    return colors[priority] || 'bg-gray-500';
  };

  const getPriorityStyle = (priority) => {
    const styles = {
      urgent: 'bg-red-100 text-red-800',
      high: 'bg-orange-100 text-orange-800',
      medium: 'bg-blue-100 text-blue-800',
      low: 'bg-green-100 text-green-800'
    };
    return styles[priority] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="bg-gray-100 min-h-screen">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center">
            <div className="flex items-center">
              <div className="flex-shrink-0 h-10 w-10 bg-blue-600 rounded-full flex items-center justify-center text-white">
                <User size={20} />
              </div>
              <div className="ml-3">
                <h1 className="text-xl font-bold text-gray-900">{tech.name}</h1>
                <p className="text-sm text-gray-500">{tech.title} | ID: {tech.id}</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="text-right">
                <p className="text-sm text-gray-500">Jobs Today</p>
                <p className="text-lg font-medium">{tech.stats.completed} / {tech.stats.total}</p>
              </div>
              <button className="p-2 rounded-full text-gray-400 hover:text-gray-500 hover:bg-gray-100">
                <Settings size={20} />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Search and Filters */}
      <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center space-y-4 sm:space-y-0">
          {/* Search */}
          <div className="relative w-full sm:w-64">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search size={18} className="text-gray-400" />
            </div>
            <input
              type="text"
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="Search jobs..."
            />
          </div>

          {/* Filter Controls */}
          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <button className="flex items-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
              <Filter size={16} className="mr-2" />
              Filter
            </button>
          </div>
        </div>

        {/* Priority Filter Pills */}
        <div className="mt-4 flex flex-wrap gap-2">
          {['all', 'urgent', 'high', 'medium', 'low'].map(filter => (
            <button
              key={filter}
              onClick={() => {
                setActiveFilter(filter);
                setActiveIndex(0); // Reset to first card when filter changes
              }}
              className={`px-3 py-1 rounded-full text-sm font-medium ${
                activeFilter === filter
                  ? filter === 'all' 
                    ? 'bg-gray-900 text-white' 
                    : `${filter === 'urgent' ? 'bg-red-600' : filter === 'high' ? 'bg-orange-600' : filter === 'medium' ? 'bg-blue-600' : 'bg-green-600'} text-white`
                  : `${filter === 'all' ? 'bg-gray-200 text-gray-700 hover:bg-gray-300' : getPriorityStyle(filter)}`
              }`}
            >
              {filter === 'all' ? 'All Jobs' : filter.charAt(0).toUpperCase() + filter.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Stacked Job Cards */}
      <div className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-medium text-gray-900">Active Jobs ({filteredJobs.length})</h2>
          <span className="text-sm text-gray-500">
            Card {activeIndex + 1} of {filteredJobs.length}
          </span>
        </div>

        {filteredJobs.length > 0 ? (
          <div className="relative h-96 flex justify-center items-center">
            {/* Card Navigation Controls */}
            <button 
              onClick={prevJob}
              disabled={filteredJobs.length <= 1}
              className="absolute left-0 top-1/2 -translate-y-1/2 z-20 p-2 rounded-full bg-white shadow-md text-gray-800 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft size={20} />
            </button>
            
            <button 
              onClick={nextJob}
              disabled={filteredJobs.length <= 1}
              className="absolute right-0 top-1/2 -translate-y-1/2 z-20 p-2 rounded-full bg-white shadow-md text-gray-800 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronRight size={20} />
            </button>

            {/* Create the stacked card effect */}
            {filteredJobs.map((job, index) => {
              // Determine if this card is the active one or one of the "stack" cards
              const isActive = index === activeIndex;
              const isNext = (index === (activeIndex + 1) % filteredJobs.length) || 
                             (index === (activeIndex + 2) % filteredJobs.length);
              const isPrevious = (index === (activeIndex - 1 + filteredJobs.length) % filteredJobs.length) || 
                                 (index === (activeIndex - 2 + filteredJobs.length) % filteredJobs.length);
              
              // Only render visible cards (active, and a few before/after)
              if (!isActive && !isNext && !isPrevious) {
                return null;
              }
              
              // Calculate styling for position in the stack
              let cardStyles = "absolute transition-all duration-300 ease-in-out bg-white rounded-xl shadow-lg w-full max-w-2xl";
              let zIndex = 0;
              let transform = "";
              
              if (isActive) {
                cardStyles += " z-10";
                zIndex = 10;
                transform = "translate(0, 0) rotate(0deg)";
              } else if (isPrevious) {
                cardStyles += " z-0 opacity-70";
                zIndex = index === (activeIndex - 1 + filteredJobs.length) % filteredJobs.length ? 1 : 0;
                transform = `translate(-${zIndex * 16}px, ${zIndex * 8}px) rotate(-${zIndex * 2}deg)`;
              } else if (isNext) {
                cardStyles += " z-0 opacity-70";
                zIndex = index === (activeIndex + 1) % filteredJobs.length ? 1 : 0;
                transform = `translate(${zIndex * 16}px, ${zIndex * 8}px) rotate(${zIndex * 2}deg)`;
              }
              
              return (
                <div 
                  key={job.id}
                  className={cardStyles}
                  style={{ 
                    transform,
                    zIndex
                  }}
                >
                  {/* Priority Bar */}
                  <div className={`h-2 w-full rounded-t-xl ${getPriorityColor(job.priority)}`}></div>
                  
                  <div className="p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <div className="flex items-center">
                          <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium ${getPriorityStyle(job.priority)}`}>
                            {job.priority.charAt(0).toUpperCase() + job.priority.slice(1)}
                          </span>
                          <span className="ml-2 text-xs text-gray-500">
                            {job.id}
                          </span>
                        </div>
                        <h3 className="mt-2 text-xl font-bold text-gray-900">{job.title}</h3>
                      </div>
                      <div className="flex items-center text-sm text-gray-500">
                        <Clock size={16} className="mr-1 text-gray-400" />
                        {job.time}
                      </div>
                    </div>
                    
                    <p className="mb-6 text-gray-600">{job.desc}</p>
                    
                    <div className="mb-6 space-y-2">
                      <div className="flex items-center text-sm text-gray-500">
                        <MapPin size={16} className="mr-2 text-gray-400 flex-shrink-0" />
                        <div>
                          <div className="font-medium">{job.customer}</div>
                          <div>{job.address}</div>
                        </div>
                      </div>
                      <div className="flex items-center text-sm text-gray-500">
                        <Calendar size={16} className="mr-2 text-gray-400" />
                        <div>Due: <span className="font-medium">{job.dueDate}</span></div>
                      </div>
                    </div>
                    
                    <div className="flex justify-between items-center mt-8">
                      <div className="flex space-x-2">
                        <button className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-sm font-medium rounded-md">
                          Reassign
                        </button>
                      </div>
                      <button className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-md">
                        Start Job
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="bg-white rounded-xl shadow-lg p-8 text-center">
            <p className="text-gray-600">No jobs match this filter.</p>
          </div>
        )}
        
        {/* Card Count Indicators */}
        {filteredJobs.length > 0 && (
          <div className="flex justify-center mt-6 space-x-2">
            {filteredJobs.map((_, index) => (
              <button
                key={index}
                onClick={() => setActiveIndex(index)}
                className={`h-2 w-${index === activeIndex ? '6' : '2'} rounded-full ${
                  index === activeIndex ? 'bg-blue-600' : 'bg-gray-300'
                }`}
                aria-label={`Go to card ${index + 1}`}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}